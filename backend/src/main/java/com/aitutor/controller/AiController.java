package com.aitutor.controller;

import com.aitutor.aspect.RateLimit;
import com.aitutor.common.R;
import com.aitutor.dto.ChatDTO;
import com.aitutor.entity.CodeHelpRecord;
import com.aitutor.entity.QaRecord;
import com.aitutor.mapper.CodeHelpRecordMapper;
import com.aitutor.mapper.QaRecordMapper;
import com.aitutor.security.AuthUtil;
import com.aitutor.service.AiService;
import com.aitutor.ws.LearningWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final QaRecordMapper qaRecordMapper;
    private final CodeHelpRecordMapper codeHelpRecordMapper;
    private final LearningWebSocketHandler webSocketHandler;

    /** RAG 文本问答 */
    @RateLimit(key = "ai-chat", limit = 30, windowSec = 3600)
    @PostMapping("/chat")
    public R<Map<String, String>> chat(@RequestBody ChatDTO dto) {
        Long userId = AuthUtil.currentUserId();
        Map<String, String> result = aiService.chat(dto.getQuestion(), dto.getLessonPackId());

        QaRecord record = new QaRecord();
        record.setUserId(userId);
        record.setLessonPackId(dto.getLessonPackId());
        record.setQuestion(dto.getQuestion());
        record.setAnswer(result.get("answer"));
        record.setSourceRefs(result.get("source"));
        record.setFollowUpCount(0);
        record.setIsSyncedToTeacher(1);
        record.setHasImage(0);
        qaRecordMapper.insert(record);

        broadcast("AI 提问", dto.getQuestion(), userId);
        return R.ok(result);
    }

    /** 智能问答：自动识别问题类型（问答/报错诊断/函数查询） */
    @RateLimit(key = "ai-chat", limit = 30, windowSec = 3600)
    @PostMapping("/smart")
    public R<Map<String, String>> smart(@RequestBody ChatDTO dto) {
        Long userId = AuthUtil.currentUserId();
        Map<String, String> result = aiService.smartChat(dto.getQuestion(), dto.getLessonPackId());

        QaRecord record = new QaRecord();
        record.setUserId(userId);
        record.setLessonPackId(dto.getLessonPackId());
        record.setQuestion(dto.getQuestion());
        record.setAnswer(result.get("answer"));
        record.setSourceRefs(result.get("source"));
        record.setFollowUpCount(0);
        record.setIsSyncedToTeacher(1);
        record.setHasImage(0);
        qaRecordMapper.insert(record);

        String type = result.getOrDefault("type", "chat");
        broadcast("code".equals(type) ? "代码排错" : "func".equals(type) ? "函数查询" : "AI 提问",
                dto.getQuestion().substring(0, Math.min(50, dto.getQuestion().length())), userId);
        return R.ok(result);
    }

    /** 图片分析（新增需求：学生上传图片问 AI 助手） */
    @RateLimit(key = "ai-image", limit = 10, windowSec = 3600)
    @PostMapping("/image-analysis")
    public R<Map<String, String>> imageAnalysis(@RequestParam("file") MultipartFile file,
                                                @RequestParam(value = "question", required = false) String question,
                                                @RequestParam(value = "lessonPackId", required = false) Long lessonPackId) {
        Long userId = AuthUtil.currentUserId();
        try {
            String mimeType = file.getContentType();
            if (mimeType == null || !mimeType.startsWith("image/")) {
                return R.fail(400, "请上传图片文件");
            }
            byte[] bytes = file.getBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            Map<String, String> result = aiService.imageAnalysis(base64, mimeType, question);

            QaRecord record = new QaRecord();
            record.setUserId(userId);
            record.setLessonPackId(lessonPackId);
            record.setQuestion(question == null || question.isBlank() ? "[图片提问]" : question);
            record.setAnswer(result.get("answer"));
            record.setSourceRefs(result.get("source"));
            record.setFollowUpCount(0);
            record.setIsSyncedToTeacher(1);
            record.setHasImage(1);
            qaRecordMapper.insert(record);

            broadcast("图片提问", record.getQuestion(), userId);
            return R.ok(result);
        } catch (Exception e) {
            log.error("图片分析失败", e);
            return R.fail(500, "图片分析失败：" + e.getMessage());
        }
    }

    /** 文件分析：支持图片/文本/代码等多种文件，自动识别类型 */
    @RateLimit(key = "ai-chat", limit = 30, windowSec = 3600)
    @PostMapping("/file-analysis")
    public R<Map<String, String>> fileAnalysis(@RequestParam("file") MultipartFile file,
                                               @RequestParam(value = "question", required = false) String question,
                                               @RequestParam(value = "lessonPackId", required = false) Long lessonPackId) {
        Long userId = AuthUtil.currentUserId();
        try {
            if (file == null || file.isEmpty()) return R.fail(400, "请选择文件");
            String filename = file.getOriginalFilename() == null ? "文件" : file.getOriginalFilename();
            String mimeType = file.getContentType();
            byte[] bytes = file.getBytes();
            Map<String, String> result = aiService.fileAnalysis(bytes, filename, mimeType, question, lessonPackId);

            boolean isImage = mimeType != null && mimeType.startsWith("image/");
            QaRecord record = new QaRecord();
            record.setUserId(userId);
            record.setLessonPackId(lessonPackId);
            record.setQuestion(question == null || question.isBlank() ? "[文件:" + filename + "]" : question);
            record.setAnswer(result.get("answer"));
            record.setSourceRefs(result.get("source"));
            record.setFollowUpCount(0);
            record.setIsSyncedToTeacher(1);
            record.setHasImage(isImage ? 1 : 0);
            qaRecordMapper.insert(record);

            broadcast(isImage ? "图片提问" : "文件提问", record.getQuestion(), userId);
            return R.ok(result);
        } catch (Exception e) {
            log.error("文件分析失败", e);
            return R.fail(500, "文件分析失败：" + e.getMessage());
        }
    }

    /** 代码报错诊断 */
    @RateLimit(key = "ai-code", limit = 20, windowSec = 3600)
    @PostMapping("/code-help")
    public R<Map<String, String>> codeHelp(@RequestBody ChatDTO dto) {
        Long userId = AuthUtil.currentUserId();
        Map<String, String> result = aiService.codeHelp(dto.getQuestion(), dto.getLessonPackId());

        CodeHelpRecord record = new CodeHelpRecord();
        record.setUserId(userId);
        record.setLessonPackId(dto.getLessonPackId());
        record.setErrorText(dto.getQuestion());
        record.setDiagnosis(result.get("answer"));
        record.setSuggestion(result.get("answer"));
        record.setSourceRefs(result.get("source"));
        codeHelpRecordMapper.insert(record);

        broadcast("代码排错", dto.getQuestion().substring(0, Math.min(50, dto.getQuestion().length())), userId);
        return R.ok(result);
    }

    /** 函数查询 */
    @RateLimit(key = "ai-func", limit = 30, windowSec = 3600)
    @PostMapping("/function-query")
    public R<Map<String, String>> functionQuery(@RequestBody ChatDTO dto) {
        AuthUtil.currentUserId();
        return R.ok(aiService.functionQuery(dto.getQuestion()));
    }

    private void broadcast(String type, String detail, Long userId) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", type);
        msg.put("userId", userId);
        msg.put("detail", detail);
        msg.put("time", System.currentTimeMillis());
        webSocketHandler.broadcast(msg);
    }
}
