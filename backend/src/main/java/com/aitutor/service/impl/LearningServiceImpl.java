package com.aitutor.service.impl;

import com.aitutor.common.Constants;
import com.aitutor.entity.*;
import com.aitutor.mapper.*;
import com.aitutor.service.LearningService;
import com.aitutor.service.OnlineService;
import com.aitutor.ws.LearningWebSocketHandler;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningServiceImpl implements LearningService {

    private final StudentOperationMapper operationMapper;
    private final StudentMaterialViewMapper materialViewMapper;
    private final QaRecordMapper qaRecordMapper;
    private final ChallengeSubmissionMapper challengeMapper;
    private final CodeHelpRecordMapper codeHelpRecordMapper;
    private final UserMapper userMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final LearningStatsMapper learningStatsMapper;
    private final ConfusionClusterMapper confusionClusterMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final LearningWebSocketHandler webSocketHandler;
    private final OnlineService onlineService;

    @Override
    public void recordOperation(Long userId, Long lessonPackId, Long materialId,
                                String actionType, String actionDetail, Integer duration) {
        StudentOperation op = new StudentOperation();
        op.setUserId(userId);
        op.setLessonPackId(lessonPackId);
        op.setMaterialId(materialId);
        op.setActionType(actionType);
        op.setActionDetail(actionDetail);
        op.setDuration(duration == null ? 0 : duration);
        op.setCreateTime(LocalDateTime.now());
        operationMapper.insert(op);

        // Redis 实时计数
        try {
            onlineService.touch(userId);
            redisTemplate.opsForValue().increment("stats:operations");
            if ("question".equals(actionType)) {
                redisTemplate.opsForValue().increment("stats:questions");
            }
            if ("challenge".equals(actionType)) {
                redisTemplate.opsForValue().increment("stats:challenges");
            }
            // 最近动态列表（用于实时流）
            redisTemplate.opsForList().leftPush("stats:stream",
                    actionType + "|" + userId + "|" + (actionDetail == null ? "" : actionDetail) + "|" + System.currentTimeMillis());
            redisTemplate.opsForList().trim("stats:stream", 0, 199);
            redisTemplate.expire("stats:stream", 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Redis 计数失败", e);
        }

        // WebSocket 广播
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", actionType);
        msg.put("userId", userId);
        msg.put("detail", actionDetail);
        msg.put("time", System.currentTimeMillis());
        webSocketHandler.broadcast(msg);
    }

    @Override
    public Map<String, Object> overview(Long lessonPackId) {
        Map<String, Object> result = new HashMap<>();
        long online = onlineService.onlineCount();
        Long operations = getLong("stats:operations");
        Long questions = getLong("stats:questions");
        Long challenges = getLong("stats:challenges");
        result.put("online", online);
        result.put("operations", operations == null ? 0 : operations);
        result.put("questions", questions == null ? 0 : questions);
        result.put("challenges", challenges == null ? 0 : challenges);

        Long studentCount = userMapper.selectCount(
                new QueryWrapper<User>().eq("role", Constants.ROLE_STUDENT));
        result.put("studentCount", studentCount);
        result.put("updateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        return result;
    }

    @Override
    public List<Map<String, Object>> topConfusion(Long lessonPackId) {
        QueryWrapper<QaRecord> qw = new QueryWrapper<>();
        if (lessonPackId != null) qw.eq("lesson_pack_id", lessonPackId);
        List<QaRecord> records = qaRecordMapper.selectList(qw);
        // 简单按知识关键词聚类：按问题长度归一，此处按知识点表关键词匹配统计
        Map<String, Integer> counter = new HashMap<>();
        for (QaRecord r : records) {
            String q = r.getQuestion() == null ? "" : r.getQuestion();
            String kw = matchKeyword(q);
            counter.merge(kw, 1, Integer::sum);
        }
        return counter.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

    private String matchKeyword(String q) {
        List<KnowledgePoint> points = knowledgePointMapper.selectList(null);
        String best = "其他";
        int bestScore = 0;
        for (KnowledgePoint kp : points) {
            if (kp.getKeywords() == null) continue;
            int score = 0;
            for (String kw : kp.getKeywords().split(",")) {
                if (q.contains(kw.trim())) score++;
            }
            if (score > bestScore) {
                bestScore = score;
                best = kp.getName();
            }
        }
        return best;
    }

    @Override
    public List<Map<String, Object>> mastery(Long lessonPackId) {
        List<KnowledgePoint> points = knowledgePointMapper.selectList(
                new QueryWrapper<KnowledgePoint>().eq(lessonPackId != null, "lesson_pack_id", lessonPackId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (KnowledgePoint kp : points) {
            QueryWrapper<LearningStats> qw = new QueryWrapper<LearningStats>()
                    .eq("knowledge_point_id", kp.getId());
            List<LearningStats> stats = learningStatsMapper.selectList(qw);
            double avg = stats.isEmpty() ? 0 :
                    stats.stream().mapToDouble(s -> s.getMasteryScore() == null ? 0 : s.getMasteryScore().doubleValue())
                            .average().orElse(0);
            Map<String, Object> m = new HashMap<>();
            m.put("knowledgePoint", kp.getName());
            m.put("mastery", Math.round(avg * 100) / 100.0);
            m.put("studentCount", stats.size());
            result.add(m);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> stream(int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            List<Object> raw = redisTemplate.opsForList().range("stats:stream", 0, limit - 1);
            if (raw != null) {
                for (Object o : raw) {
                    String s = String.valueOf(o);
                    String[] parts = s.split("\\|", 4);
                    Map<String, Object> m = new HashMap<>();
                    m.put("type", parts.length > 0 ? parts[0] : "");
                    m.put("userId", parts.length > 1 ? parts[1] : "");
                    m.put("detail", parts.length > 2 ? parts[2] : "");
                    m.put("time", parts.length > 3 ? parts[3] : "");
                    result.add(m);
                }
            }
        } catch (Exception e) {
            log.warn("读取实时流失败", e);
        }
        if (result.isEmpty()) {
            // 兜底：查最近操作记录
            List<StudentOperation> ops = operationMapper.selectList(
                    new QueryWrapper<StudentOperation>().orderByDesc("id").last("limit " + limit));
            for (StudentOperation op : ops) {
                Map<String, Object> m = new HashMap<>();
                m.put("type", op.getActionType());
                m.put("userId", String.valueOf(op.getUserId()));
                m.put("detail", op.getActionDetail());
                m.put("time", op.getCreateTime() == null ? "" : op.getCreateTime().toString());
                result.add(m);
            }
        }
        return result;
    }

    private Long getLong(String key) {
        try {
            Object v = redisTemplate.opsForValue().get(key);
            if (v == null) return 0L;
            return Long.valueOf(String.valueOf(v));
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override
    public Map<String, Object> selfStats(Long userId) {
        Map<String, Object> result = new HashMap<>();
        Long questionCount = qaRecordMapper.selectCount(
                new QueryWrapper<QaRecord>().eq("user_id", userId));
        Long challengeCount = challengeMapper.selectCount(
                new QueryWrapper<ChallengeSubmission>().eq("user_id", userId));
        Long operationCount = operationMapper.selectCount(
                new QueryWrapper<StudentOperation>().eq("user_id", userId));
        result.put("questionCount", questionCount);
        result.put("challengeCount", challengeCount);
        result.put("operationCount", operationCount);

        // 按知识点掌握度（若有 learning_stats 记录则汇总，否则返回空列表）
        List<LearningStats> stats = learningStatsMapper.selectList(
                new QueryWrapper<LearningStats>().eq("user_id", userId));
        List<Map<String, Object>> mastery = new ArrayList<>();
        for (LearningStats s : stats) {
            KnowledgePoint kp = knowledgePointMapper.selectById(s.getKnowledgePointId());
            if (kp == null) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("knowledgePoint", kp.getName());
            m.put("mastery", s.getMasteryScore() == null ? 0 : s.getMasteryScore().doubleValue());
            m.put("participation", s.getParticipationScore() == null ? 0 : s.getParticipationScore().doubleValue());
            mastery.add(m);
        }
        result.put("mastery", mastery);
        return result;
    }

    @Override
    public List<Map<String, Object>> errorRanking(Long lessonPackId) {
        // 从代码排错记录中按关键词统计错误类型
        List<CodeHelpRecord> records = codeHelpRecordMapper.selectList(
                new QueryWrapper<CodeHelpRecord>().eq(lessonPackId != null, "lesson_pack_id", lessonPackId));
        Map<String, Map<String, Object>> counter = new HashMap<>();
        for (CodeHelpRecord r : records) {
            String type = classifyError(r.getErrorText());
            Map<String, Object> m = counter.computeIfAbsent(type, k -> {
                Map<String, Object> mm = new HashMap<>();
                mm.put("type", k);
                mm.put("count", 0);
                mm.put("students", new java.util.HashSet<Long>());
                return mm;
            });
            m.put("count", (int) m.get("count") + 1);
            ((java.util.HashSet<Long>) m.get("students")).add(r.getUserId());
        }
        return counter.values().stream()
                .sorted((a, b) -> (int) b.get("count") - (int) a.get("count"))
                .limit(10)
                .map(m -> {
                    Map<String, Object> r = new HashMap<>();
                    r.put("type", m.get("type"));
                    r.put("count", m.get("count"));
                    r.put("studentCount", ((java.util.HashSet<Long>) m.get("students")).size());
                    return r;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private String classifyError(String errorText) {
        if (errorText == null) return "其他";
        if (errorText.contains("BGR") || errorText.contains("RGB") || errorText.contains("颜色")) return "BGR/RGB 混淆";
        if (errorText.contains("None") || errorText.contains("NoneType") || errorText.contains("shape")) return "图像路径/None";
        if (errorText.contains("overflow") || errorText.contains("溢出") || errorText.contains("uint8")) return "数值溢出";
        if (errorText.contains("ksize") || errorText.contains("奇数")) return "核大小非法";
        if (errorText.contains("channel") || errorText.contains("通道")) return "通道不匹配";
        if (errorText.contains("SIFT") || errorText.contains("xfeatures2d")) return "SIFT 模块";
        if (errorText.contains("threshold") || errorText.contains("阈值")) return "阈值设置";
        return "其他";
    }

    @Override
    public List<Map<String, Object>> anomalies(Long lessonPackId) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<User> students = userMapper.selectList(
                new QueryWrapper<User>().eq("role", Constants.ROLE_STUDENT));
        for (User s : students) {
            // 挑战多次失败（提交少但提问多）
            Long qCount = qaRecordMapper.selectCount(new QueryWrapper<QaRecord>().eq("user_id", s.getId()));
            Long opCount = operationMapper.selectCount(new QueryWrapper<StudentOperation>().eq("user_id", s.getId()));
            // 反复问同一问题（简化为提问数过高）
            if (qCount >= 10) {
                Map<String, Object> m = new HashMap<>();
                m.put("studentId", s.getId());
                m.put("studentName", s.getRealName() == null ? s.getUsername() : s.getRealName());
                m.put("username", s.getUsername());
                m.put("type", "反复提问");
                m.put("detail", "提问 " + qCount + " 次，可能存在困惑点");
                result.add(m);
            }
            // 长时间无操作（操作数为 0 但已登录过）
            if (s.getLastLoginTime() != null && (opCount == null || opCount == 0)) {
                Map<String, Object> m = new HashMap<>();
                m.put("studentId", s.getId());
                m.put("studentName", s.getRealName() == null ? s.getUsername() : s.getRealName());
                m.put("username", s.getUsername());
                m.put("type", "无操作");
                m.put("detail", "登录后无任何学习操作");
                result.add(m);
            }
        }
        return result;
    }

    @Override
    public Map<String, Object> lessonReport(Long lessonPackId) {
        Map<String, Object> result = new HashMap<>();
        // 困惑点 Top5 作为薄弱知识点
        List<Map<String, Object>> confusion = topConfusion(lessonPackId);
        result.put("weakPoints", confusion);
        result.put("errorRanking", errorRanking(lessonPackId));
        result.put("overview", overview(lessonPackId));
        result.put("suggestion", "建议对以下知识点重点讲解：" +
                confusion.stream().limit(3).map(c -> String.valueOf(c.get("name")))
                        .reduce((a, b) -> a + "、" + b).orElse("暂无明显薄弱点"));
        result.put("generateTime", LocalDateTime.now().toString());
        return result;
    }

    @Override
    public List<Map<String, Object>> listOperations(Long lessonPackId, Long studentId, String actionType, int limit) {
        QueryWrapper<StudentOperation> qw = new QueryWrapper<>();
        qw.eq(lessonPackId != null, "lesson_pack_id", lessonPackId)
          .eq(studentId != null, "user_id", studentId)
          .eq(actionType != null && !actionType.isBlank(), "action_type", actionType)
          .orderByDesc("id");
        qw.last("limit " + Math.max(1, Math.min(limit, 500)));
        List<StudentOperation> ops = operationMapper.selectList(qw);
        List<Map<String, Object>> result = new ArrayList<>();
        for (StudentOperation op : ops) {
            User u = userMapper.selectById(op.getUserId());
            Map<String, Object> m = new HashMap<>();
            m.put("id", op.getId());
            m.put("userId", op.getUserId());
            m.put("studentName", u == null ? "未知" : (u.getRealName() == null ? u.getUsername() : u.getRealName()));
            m.put("username", u == null ? "" : u.getUsername());
            m.put("lessonPackId", op.getLessonPackId());
            m.put("materialId", op.getMaterialId());
            m.put("actionType", op.getActionType());
            m.put("actionDetail", op.getActionDetail());
            m.put("duration", op.getDuration());
            m.put("time", op.getCreateTime());
            result.add(m);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> wordCloud(Long lessonPackId, int limit) {
        List<QaRecord> records = qaRecordMapper.selectList(
                new QueryWrapper<QaRecord>().eq(lessonPackId != null, "lesson_pack_id", lessonPackId));
        Map<String, Integer> counter = new HashMap<>();
        for (QaRecord r : records) {
            for (String word : tokenize(r.getQuestion())) {
                counter.merge(word, 1, Integer::sum);
            }
        }
        return counter.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit <= 0 ? 50 : limit)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", e.getKey());
                    m.put("value", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

    /** 简单中文分词：基于课程关键词表 + 常见专业词匹配 */
    private List<String> tokenize(String question) {
        List<String> words = new ArrayList<>();
        if (question == null || question.isBlank()) return words;
        // 先用知识点关键词匹配
        List<KnowledgePoint> points = knowledgePointMapper.selectList(null);
        List<String> keywords = new ArrayList<>();
        for (KnowledgePoint kp : points) {
            if (kp.getKeywords() != null) {
                for (String kw : kp.getKeywords().split(",")) {
                    String k = kw.trim();
                    if (!k.isEmpty() && !keywords.contains(k)) keywords.add(k);
                }
            }
        }
        // 常见专业词补充
        String[] common = {
                "灰度", "直方图", "均衡化", "采样", "量化", "分辨率", "像素", "对比度",
                "滤波", "卷积", "傅里叶", "频域", "空间域", "形态学", "腐蚀", "膨胀",
                "分割", "阈值", "边缘", "Canny", "Otsu", "SIFT", "LBP", "HOG",
                "特征", "噪声", "中值滤波", "高斯", "均值", "锐化", "平滑", "BGR", "RGB",
                "OpenCV", "imread", "cvtColor", "calcHist", "GaussianBlur", "通道", "矩阵"
        };
        for (String c : common) {
            if (!keywords.contains(c)) keywords.add(c);
        }
        // 匹配关键词
        for (String kw : keywords) {
            if (question.contains(kw)) words.add(kw);
        }
        // 英文单词提取（如 cv2.Canny 提取 Canny）
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[A-Za-z][A-Za-z0-9]{1,}").matcher(question);
        while (m.find()) {
            String w = m.group();
            if (w.length() >= 2 && !w.equalsIgnoreCase("the") && !w.equalsIgnoreCase("and")) {
                words.add(w);
            }
        }
        return words;
    }
}
