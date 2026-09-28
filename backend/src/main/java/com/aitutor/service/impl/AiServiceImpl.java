package com.aitutor.service.impl;

import com.aitutor.ai.AiClient;
import com.aitutor.ai.PromptTemplates;
import com.aitutor.ai.RagHit;
import com.aitutor.ai.RagService;
import com.aitutor.service.AiService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiClient aiClient;
    private final RagService ragService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, String> chat(String question, Long lessonPackId) {
        List<RagHit> hits = ragService.search(question, 5);
        String context = PromptTemplates.buildContext(hits);
        String system = PromptTemplates.ragSystem(context);
        String answer = aiClient.chat(List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", question)
        ), 0.3);
        Map<String, String> result = new HashMap<>();
        result.put("answer", answer);
        result.put("source", PromptTemplates.buildSource(hits));
        return result;
    }

    @Override
    public Map<String, String> smartChat(String question, Long lessonPackId) {
        String type = detectType(question);
        Map<String, String> result;
        if ("code".equals(type)) {
            result = codeHelp(question, lessonPackId);
        } else if ("func".equals(type)) {
            result = functionQuery(question);
        } else {
            result = chat(question, lessonPackId);
        }
        result.put("type", type);
        return result;
    }

    /** 自动识别问题类型：报错诊断 / 函数查询 / 普通问答 */
    private String detectType(String q) {
        if (q == null || q.isBlank()) return "chat";
        String s = q.toLowerCase();
        // 报错特征
        String[] errMarkers = {
                "traceback", "exception", "error:", "attributeerror", "typeerror", "valueerror",
                "modulenotfound", "filenotfound", "assertion failed", "none' object", "nonetype",
                "报错", "错误", "异常", "失败", "崩溃", "无法运行", "运行不了", "出错了"
        };
        for (String m : errMarkers) {
            if (s.contains(m)) return "code";
        }
        // 函数查询特征：cv2.xxx 函数名 + "怎么用/用法/参数/函数"等
        if (s.matches(".*cv2\\.[a-zA-Z_]+.*") || s.matches(".*np\\.[a-zA-Z_]+.*")
                || s.matches(".*cv\\.\\w+.*")) {
            return "func";
        }
        if ((s.contains("怎么用") || s.contains("用法") || s.contains("参数") || s.contains("返回值")
                || s.contains("函数") || s.contains("签名") || s.contains("作用"))
                && (s.contains("cv2") || s.contains("numpy") || s.contains("np.") || s.contains("opencv"))) {
            return "func";
        }
        return "chat";
    }

    @Override
    public Map<String, String> codeHelp(String errorText, Long lessonPackId) {
        List<RagHit> hits = ragService.search(errorText, 4);
        String context = PromptTemplates.buildContext(hits);
        String system = PromptTemplates.codeHelpSystem(context);
        String answer = aiClient.chat(List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", "报错信息如下：\n" + errorText)
        ), 0.3);
        Map<String, String> result = new HashMap<>();
        result.put("answer", answer);
        result.put("source", PromptTemplates.buildSource(hits));
        return result;
    }

    @Override
    public Map<String, String> functionQuery(String funcName) {
        List<RagHit> hits = ragService.search(funcName, 4);
        String context = PromptTemplates.buildContext(hits);
        String system = PromptTemplates.functionQuerySystem(context);
        String answer = aiClient.chat(List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", "请介绍函数：" + funcName)
        ), 0.2);
        Map<String, String> result = new HashMap<>();
        result.put("answer", answer);
        result.put("source", PromptTemplates.buildSource(hits));
        return result;
    }

    @Override
    public Map<String, Object> prepareLesson(String teachingPlan) {
        String answer = aiClient.chat(List.of(
                Map.of("role", "system", "content", PromptTemplates.prepareSystem()),
                Map.of("role", "user", "content", "教案内容：\n" + teachingPlan)
        ), 0.2);
        return parseJson(answer);
    }

    @Override
    public Map<String, String> imageAnalysis(String base64Image, String mimeType, String question) {
        String userText = question == null || question.isBlank()
                ? "请分析这张图像的内容与含义。"
                : "学生的问题：" + question + "。请结合这张图像进行分析回答。";
        String answer = aiClient.chatWithImage(PromptTemplates.visionSystem(), userText, base64Image, mimeType);
        Map<String, String> result = new HashMap<>();
        result.put("answer", answer);
        result.put("source", "视觉模型");
        return result;
    }

    @Override
    public Map<String, String> fileAnalysis(byte[] bytes, String filename, String mimeType, String question, Long lessonPackId) {
        // 图片 → 视觉模型
        if (mimeType != null && mimeType.startsWith("image/")) {
            String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
            String userText = question == null || question.isBlank()
                    ? "请分析这张图像的内容与含义。"
                    : "学生的问题：" + question + "。请结合这张图像进行分析回答。";
            String answer = aiClient.chatWithImage(PromptTemplates.visionSystem(), userText, base64, mimeType);
            Map<String, String> r = new HashMap<>();
            r.put("answer", answer);
            r.put("source", "视觉模型");
            return r;
        }
        // 文本类文件 → 读取内容交给大模型
        String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        // 限制长度，避免超出 token
        if (content.length() > 8000) {
            content = content.substring(0, 8000) + "\n...(内容过长已截断)";
        }
        String userText = (question == null || question.isBlank() ? "请阅读并解读这份文件的内容。" : question)
                + "\n\n文件名：" + filename + "\n文件内容：\n" + content;
        List<RagHit> hits = ragService.search(question == null ? "" : question, 5);
        String system = PromptTemplates.ragSystem(PromptTemplates.buildContext(hits));
        String answer = aiClient.chat(List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", userText)
        ), 0.3);
        Map<String, String> r = new HashMap<>();
        r.put("answer", answer);
        r.put("source", PromptTemplates.buildSource(hits));
        return r;
    }

    private Map<String, Object> parseJson(String raw) {
        try {
            String json = raw.trim();
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("备课助手 JSON 解析失败，返回原文");
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("raw", raw);
            return fallback;
        }
    }
}
