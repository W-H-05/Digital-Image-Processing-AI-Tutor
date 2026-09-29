package com.aitutor.service;

import com.aitutor.ai.RagHit;

import java.util.List;
import java.util.Map;

/**
 * AI 服务接口
 */
public interface AiService {

    /** RAG 文本问答，返回 {answer, source} */
    Map<String, String> chat(String question, Long lessonPackId);

    /** 智能问答：自动识别问题类型（问答/报错/函数），返回 {answer, source, type} */
    Map<String, String> smartChat(String question, Long lessonPackId);

    /** 智能问答（流式）：自动识别类型，逐块回调 onChunk（正文）、onReason（思考过程），返回 {source, type} */
    Map<String, String> smartChatStream(String question, Long lessonPackId,
                                        java.util.function.Consumer<String> onChunk,
                                        java.util.function.Consumer<String> onReason);

    /** 代码报错诊断 */
    Map<String, String> codeHelp(String errorText, Long lessonPackId);

    /** 函数查询 */
    Map<String, String> functionQuery(String funcName);

    /** 备课助手：从教案抽取结构化内容，返回解析后的 Map */
    Map<String, Object> prepareLesson(String teachingPlan);

    /** 图片分析：返回 {answer} */
    Map<String, String> imageAnalysis(String base64Image, String mimeType, String question);

    /** 文件分析（多类型：图片走视觉，文本类读取内容交给大模型）：返回 {answer, source} */
    Map<String, String> fileAnalysis(byte[] bytes, String filename, String mimeType, String question, Long lessonPackId);
}
