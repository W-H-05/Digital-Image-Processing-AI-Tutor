package com.aitutor.service;

import java.util.List;
import java.util.Map;

public interface LearningService {

    /** 记录学生操作，写库 + Redis 计数 + WebSocket 广播 */
    void recordOperation(Long userId, Long lessonPackId, Long materialId,
                         String actionType, String actionDetail, Integer duration);

    /** 学情看板：实时概览 */
    Map<String, Object> overview(Long lessonPackId);

    /** 集中困惑点 Top5 */
    List<Map<String, Object>> topConfusion(Long lessonPackId);

    /** 知识点掌握度 */
    List<Map<String, Object>> mastery(Long lessonPackId);

    /** 实时动态流 */
    List<Map<String, Object>> stream(int limit);

    /** 学生个人学情统计（掌握度、提问、操作、挑战） */
    Map<String, Object> selfStats(Long userId);

    /** 易错点排行 */
    List<Map<String, Object>> errorRanking(Long lessonPackId);

    /** 异常学习行为提醒 */
    List<Map<String, Object>> anomalies(Long lessonPackId);

    /** 课后课堂总结报告 */
    Map<String, Object> lessonReport(Long lessonPackId);

    /** 教师查看所有埋点操作记录（支持筛选） */
    List<Map<String, Object>> listOperations(Long lessonPackId, Long studentId, String actionType, int limit);

    /** 词云：统计某课次 AI 提问的词条频率 */
    List<Map<String, Object>> wordCloud(Long lessonPackId, int limit);

    /** 记录作业提交结果：更新知识掌握度、易错点、异常提醒 */
    void recordHomeworkResult(Long userId, Long lessonPackId, int score, int totalScore, List<String> wrongStems);
}
