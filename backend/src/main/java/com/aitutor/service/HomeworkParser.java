package com.aitutor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 作业题目解析器：从 txt 文本解析选择题/填空题/代码题。
 *
 * 支持格式示例：
 *   1. [单选] 灰度图像每个像素的灰度值范围是？
 *   A. 0~1
 *   B. 0~255
 *   C. -128~127
 *   D. 0~65535
 *   答案：B
 *
 *   2. [填空] 灰度直方图的横轴表示____。
 *   答案：灰度级
 *
 *   3. [代码] 使用OpenCV读取并显示一张图片。
 *   答案：略
 */
@Slf4j
@Component
public class HomeworkParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Map<String, Object>> parse(String text) {
        List<Map<String, Object>> questions = new ArrayList<>();
        if (text == null || text.isBlank()) return questions;

        String[] lines = text.split("\\r?\\n");
        Map<String, Object> current = null;
        List<String> options = new ArrayList<>();
        StringBuilder codeAnswer = new StringBuilder();
        boolean inCodeAnswer = false;

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) {
                if (inCodeAnswer) codeAnswer.append("\n");
                continue;
            }

            // 匹配题目起始行：1. [单选] 题目内容
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("^\\s*(\\d+)\\s*[.、]\\s*[\\[【]\\s*(单选|选择|填空|代码|多选)\\s*[\\]】]\\s*(.*)$")
                    .matcher(line);
            if (m.find()) {
                // 保存上一题
                if (current != null) {
                    finishQuestion(current, options, codeAnswer);
                    questions.add(current);
                }
                String typeTag = m.group(2);
                current = new LinkedHashMap<>();
                current.put("type", toType(typeTag));
                current.put("stem", m.group(3).trim());
                options = new ArrayList<>();
                codeAnswer = new StringBuilder();
                inCodeAnswer = false;
                continue;
            }

            if (current == null) continue;

            String type = String.valueOf(current.get("type"));
            // 选择题选项
            if ("choice".equals(type)) {
                java.util.regex.Matcher om = java.util.regex.Pattern
                        .compile("^\\s*([A-Ha-h])\\s*[.、．]\\s*(.*)$").matcher(line);
                if (om.find()) {
                    options.add(om.group(1).toUpperCase() + ". " + om.group(2).trim());
                    continue;
                }
            }
            // 答案行
            java.util.regex.Matcher am = java.util.regex.Pattern
                    .compile("^\\s*(答案|参考答案)\\s*[:：]\\s*(.*)$").matcher(line);
            if (am.find()) {
                String ans = am.group(2).trim();
                current.put("answer", ans);
                inCodeAnswer = false;
                if ("code".equals(type)) {
                    // 代码题答案可能是占位（略/无），后续行作为参考代码
                    if (ans.equals("略") || ans.equals("无") || ans.isEmpty()) {
                        inCodeAnswer = true;
                        codeAnswer = new StringBuilder();
                    } else {
                        codeAnswer = new StringBuilder(ans);
                    }
                }
                continue;
            }
            // 代码题的参考答案多行收集
            if ("code".equals(type) && inCodeAnswer) {
                codeAnswer.append(line).append("\n");
                continue;
            }
        }
        // 保存最后一题
        if (current != null) {
            finishQuestion(current, options, codeAnswer);
            questions.add(current);
        }
        return questions;
    }

    private String toType(String tag) {
        if (tag.contains("填空")) return "blank";
        if (tag.contains("代码")) return "code";
        return "choice"; // 单选/选择/多选 统一按 choice 处理
    }

    private void finishQuestion(Map<String, Object> q, List<String> options, StringBuilder codeAnswer) {
        String type = String.valueOf(q.get("type"));
        if ("choice".equals(type)) {
            q.put("options", new ArrayList<>(options));
        }
        if ("code".equals(type)) {
            String code = codeAnswer.toString().trim();
            if (code.isEmpty()) code = String.valueOf(q.getOrDefault("answer", "略"));
            q.put("answer", code);
        }
        if (q.get("answer") == null) {
            q.put("answer", "");
        }
        // 默认分值
        if (!q.containsKey("score")) {
            q.put("score", type.equals("code") ? 10 : 5);
        }
    }
}
