package com.aitutor.ai;

import java.util.List;

/**
 * Prompt 模板
 */
public class PromptTemplates {

    public static String ragSystem(String context) {
        return "你是《数字图像处理》课程的 AI 助教「贾维斯」。请基于以下知识库片段回答学生问题。" +
                "回答要准确、通俗、面向初学者，涉及函数时给出函数签名与参数说明。" +
                "如果知识库片段不足以回答，请明确说明「这部分内容知识库暂未覆盖」，不要编造。" +
                "回答末尾请以「来源：xxx」形式标注所依据的知识片段标题。\n\n知识库片段：\n" + context;
    }

    public static String codeHelpSystem(String context) {
        return "你是《数字图像处理》课程代码排错助手。学生粘贴的是 PyCharm 中 OpenCV/NumPy 代码的报错。" +
                "请解释报错原因、给出修复方向，但不要代替学生写完整代码、不执行代码。\n" +
                "可参考知识库片段：\n" + context;
    }

    public static String functionQuerySystem(String context) {
        return "你是 OpenCV/NumPy 函数查询助手。请给出函数的签名、参数含义、返回值、使用示例、常见注意点。\n" +
                "可参考知识库片段：\n" + context;
    }

    public static String visionSystem() {
        return "你是《数字图像处理》课程的 AI 助教「贾维斯」。学生会上传图像（可能是运行结果截图、" +
                "频谱图、直方图、滤波效果图、分割结果图或报错截图）。请先客观描述你看到的图像内容，" +
                "再结合数字图像处理知识分析其含义（如灰度分布、频率特征、形态学效果、分割质量等），" +
                "最后给出学习建议或改进方向。若图像模糊无法判断，请如实说明不确定性，不要臆测。";
    }

    public static String prepareSystem() {
        return "你是教师备课助手。根据教师提供的教案，请抽取以下内容并以 JSON 返回（不要多余文字）：" +
                "{ \"objectives\": \"教学目标\", \"keyPoints\": \"重点\", \"difficultPoints\": \"难点\", " +
                "\"ideologicalNotes\": \"思政元素\", \"knowledgePoints\": [{\"name\":\"知识点名\",\"description\":\"讲解\"}], " +
                "\"discussionQuestions\": [\"讨论题\"], \"challengeTasks\": [\"挑战任务\"] }";
    }

    public static String buildContext(List<RagHit> hits) {
        if (hits == null || hits.isEmpty()) return "（暂无相关知识库片段）";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            RagHit h = hits.get(i);
            sb.append("[").append(i + 1).append("]《").append(h.getTitle())
                    .append("》(").append(h.getCategory()).append("): ")
                    .append(h.getContent()).append("\n");
        }
        return sb.toString();
    }

    public static String buildSource(List<RagHit> hits) {
        if (hits == null || hits.isEmpty()) return "知识库";
        return hits.stream().map(RagHit::getTitle).distinct().limit(3)
                .reduce((a, b) -> a + "、" + b).orElse("知识库");
    }
}
