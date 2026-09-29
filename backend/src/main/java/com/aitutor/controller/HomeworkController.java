package com.aitutor.controller;

import com.aitutor.common.R;
import com.aitutor.entity.Homework;
import com.aitutor.entity.HomeworkSubmission;
import com.aitutor.entity.User;
import com.aitutor.mapper.HomeworkMapper;
import com.aitutor.mapper.HomeworkSubmissionMapper;
import com.aitutor.mapper.UserMapper;
import com.aitutor.security.AuthUtil;
import com.aitutor.service.HomeworkParser;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 作业管理：解析题目、学生提交、自动判分
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HomeworkController {

    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper submissionMapper;
    private final UserMapper userMapper;
    private final HomeworkParser homeworkParser;
    private final com.aitutor.service.LearningService learningService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 教师上传作业文件(txt/md)，解析成题目预览（不落库） */
    @PostMapping("/teacher/homework/parse")
    public R<Map<String, Object>> parse(@RequestParam("file") MultipartFile file) {
        AuthUtil.requireTeacher();
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            List<Map<String, Object>> questions = homeworkParser.parse(content);
            int total = 0;
            for (Map<String, Object> q : questions) {
                total += ((Number) q.getOrDefault("score", 0)).intValue();
            }
            Map<String, Object> result = new HashMap<>();
            result.put("questions", questions);
            result.put("count", questions.size());
            result.put("totalScore", total);
            return R.ok(result);
        } catch (Exception e) {
            log.error("作业解析失败", e);
            return R.fail(500, "解析失败：" + e.getMessage());
        }
    }

    /** 教师保存作业（含解析后的题目 JSON） */
    @PostMapping("/teacher/homework")
    public R<Homework> save(@RequestBody Homework homework) {
        AuthUtil.requireTeacher();
        if (homework.getTitle() == null || homework.getTitle().isBlank()) {
            return R.fail(400, "作业标题不能为空");
        }
        // 计算总分
        int total = calcTotal(homework.getQuestionsJson());
        homework.setTotalScore(total);
        homeworkMapper.insert(homework);
        return R.ok(homework);
    }

    /** 学生查看某课次的作业（不返回答案） */
    @GetMapping("/lesson-packs/{packId}/homework")
    public R<List<Map<String, Object>>> listForStudent(@PathVariable Long packId) {
        Long userId = AuthUtil.currentUserId();
        List<Homework> list = homeworkMapper.selectList(
                new QueryWrapper<Homework>().eq("lesson_pack_id", packId).orderByAsc("id"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Homework h : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", h.getId());
            m.put("title", h.getTitle());
            m.put("description", h.getDescription());
            m.put("deadline", h.getDeadline());
            m.put("totalScore", h.getTotalScore());
            // 返回题目但去掉答案
            try {
                List<Map<String, Object>> qs = objectMapper.readValue(h.getQuestionsJson(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> q : qs) {
                    q.remove("answer");
                }
                m.put("questions", qs);
            } catch (Exception ignored) {
                m.put("questions", new ArrayList<>());
            }
            // 该学生是否已提交
            Long submitted = submissionMapper.selectCount(
                    new QueryWrapper<HomeworkSubmission>().eq("homework_id", h.getId()).eq("user_id", userId));
            m.put("submitted", submitted > 0);
            result.add(m);
        }
        return R.ok(result);
    }

    /** 学生提交作业，自动判分（已提交则禁止重复提交） */
    @PostMapping("/homework/{id}/submit")
    public R<Map<String, Object>> submit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = AuthUtil.currentUserId();
        Homework h = homeworkMapper.selectById(id);
        if (h == null) return R.fail(404, "作业不存在");
        // 已提交检查
        Long submittedCount = submissionMapper.selectCount(
                new QueryWrapper<HomeworkSubmission>().eq("homework_id", id).eq("user_id", userId));
        if (submittedCount != null && submittedCount > 0) {
            return R.fail(400, "该作业已提交，不能重复提交");
        }
        try {
            List<Map<String, Object>> questions = objectMapper.readValue(h.getQuestionsJson(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            // body: { answers: { "0": "B", "1": "灰度级", "2": "代码文本" } }
            Map<String, Object> answers = (Map<String, Object>) body.get("answers");
            if (answers == null) answers = new HashMap<>();

            int score = 0;
            int autoCount = 0;
            List<Map<String, Object>> detail = new ArrayList<>();
            List<String> wrongStems = new ArrayList<>();
            for (int i = 0; i < questions.size(); i++) {
                Map<String, Object> q = questions.get(i);
                String type = String.valueOf(q.getOrDefault("type", "choice"));
                String correct = String.valueOf(q.getOrDefault("answer", "")).trim();
                String student = answers.get(String.valueOf(i)) == null ? "" : String.valueOf(answers.get(String.valueOf(i))).trim();
                int qScore = ((Number) q.getOrDefault("score", 0)).intValue();
                boolean right = false;
                if ("blank".equals(type)) {
                    // 填空题：去空格忽略大小写匹配；答案可用 | 分隔多个可接受答案
                    String[] accepted = correct.split("\\|");
                    right = false;
                    for (String a : accepted) {
                        if (student.replaceAll("\\s", "").equalsIgnoreCase(a.trim().replaceAll("\\s", ""))) {
                            right = true;
                            break;
                        }
                    }
                } else if ("choice".equals(type)) {
                    right = student.equalsIgnoreCase(correct);
                } else {
                    // 代码题：不自动判分
                    Map<String, Object> d = new HashMap<>();
                    d.put("index", i);
                    d.put("type", "code");
                    d.put("autoScored", false);
                    d.put("score", 0);
                    d.put("maxScore", qScore);
                    detail.add(d);
                    continue;
                }
                if (right) {
                    score += qScore;
                    autoCount++;
                } else {
                    // 记录错题题干，用于学情易错点统计
                    String stem = String.valueOf(q.getOrDefault("stem", ""));
                    if (!stem.isBlank()) wrongStems.add(stem);
                }
                Map<String, Object> d = new HashMap<>();
                d.put("index", i);
                d.put("type", type);
                d.put("correct", right);
                d.put("score", right ? qScore : 0);
                d.put("maxScore", qScore);
                detail.add(d);
            }

            HomeworkSubmission sub = new HomeworkSubmission();
            sub.setHomeworkId(id);
            sub.setUserId(userId);
            sub.setAnswersJson(objectMapper.writeValueAsString(answers));
            sub.setScore(score);
            sub.setAutoScored(1);
            submissionMapper.insert(sub);

            // 更新学情统计（知识掌握度、易错点、异常提醒）
            try {
                learningService.recordHomeworkResult(userId, h.getLessonPackId(), score, h.getTotalScore(), wrongStems);
            } catch (Exception ex) {
                log.warn("学情统计更新失败", ex);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("score", score);
            result.put("totalScore", h.getTotalScore());
            result.put("detail", detail);
            return R.ok(result);
        } catch (Exception e) {
            log.error("作业判分失败", e);
            return R.fail(500, "提交失败：" + e.getMessage());
        }
    }

    /** 教师查看作业提交列表（含学生提交的答案） */
    @GetMapping("/teacher/homework/{id}/submissions")
    public R<List<Map<String, Object>>> submissions(@PathVariable Long id) {
        AuthUtil.requireTeacher();
        List<HomeworkSubmission> list = submissionMapper.selectList(
                new QueryWrapper<HomeworkSubmission>().eq("homework_id", id).orderByDesc("id"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (HomeworkSubmission s : list) {
            User u = userMapper.selectById(s.getUserId());
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("studentId", s.getUserId());
            m.put("studentName", u == null ? "未知" : (u.getRealName() == null ? u.getUsername() : u.getRealName()));
            m.put("username", u == null ? "" : u.getUsername());
            m.put("score", s.getScore());
            m.put("autoScored", s.getAutoScored());
            m.put("submitTime", s.getSubmitTime());
            // 学生提交的答案（记录当时提交内容）
            m.put("answers", parseAnswers(s.getAnswersJson()));
            result.add(m);
        }
        return R.ok(result);
    }

    /** 教师查看作业题目分析：每题正确率 + 各选项学生分布 */
    @GetMapping("/teacher/homework/{id}/analysis")
    public R<Map<String, Object>> analysis(@PathVariable Long id) {
        AuthUtil.requireTeacher();
        Homework h = homeworkMapper.selectById(id);
        if (h == null) return R.fail(404, "作业不存在");
        try {
            List<Map<String, Object>> questions = objectMapper.readValue(h.getQuestionsJson(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            List<HomeworkSubmission> subs = submissionMapper.selectList(
                    new QueryWrapper<HomeworkSubmission>().eq("homework_id", id));
            List<Map<String, Object>> qa = new ArrayList<>();
            for (int i = 0; i < questions.size(); i++) {
                Map<String, Object> q = questions.get(i);
                qa.add(buildQuestionAnalysis(i, q, subs));
            }
            Map<String, Object> result = new HashMap<>();
            result.put("title", h.getTitle());
            result.put("totalScore", h.getTotalScore());
            result.put("submitCount", subs.size());
            result.put("questions", qa);
            return R.ok(result);
        } catch (Exception e) {
            log.error("作业分析失败", e);
            return R.fail(500, "分析失败：" + e.getMessage());
        }
    }

    /** 分析单道题：正确率 + 选项/答案分布 */
    private Map<String, Object> buildQuestionAnalysis(int index, Map<String, Object> q, List<HomeworkSubmission> subs) {
        Map<String, Object> m = new HashMap<>();
        String type = String.valueOf(q.getOrDefault("type", "choice"));
        String correct = String.valueOf(q.getOrDefault("answer", "")).trim();
        m.put("index", index);
        m.put("type", type);
        m.put("stem", q.get("stem"));
        m.put("score", q.get("score"));
        int correctCount = 0;
        int answeredCount = 0;

        if ("choice".equals(type)) {
            // 选择题：统计每个选项的学生
            List<String> options = (List<String>) q.get("options");
            List<Map<String, Object>> optStats = new ArrayList<>();
            if (options != null) {
                for (String opt : options) {
                    String key = String.valueOf(opt).substring(0, 1).toUpperCase();
                    Map<String, Object> os = new HashMap<>();
                    os.put("key", key);
                    os.put("text", opt);
                    os.put("isCorrect", key.equalsIgnoreCase(correct));
                    List<String> students = new ArrayList<>();
                    for (HomeworkSubmission s : subs) {
                        Map<String, Object> ans = parseAnswers(s.getAnswersJson());
                        String val = ans == null ? "" : String.valueOf(ans.getOrDefault(String.valueOf(index), "")).trim();
                        if (val.equalsIgnoreCase(key)) students.add(studentLabel(s));
                    }
                    os.put("count", students.size());
                    os.put("students", students);
                    if (key.equalsIgnoreCase(correct)) correctCount = students.size();
                    optStats.add(os);
                }
            }
            m.put("options", optStats);
            m.put("correctCount", correctCount);
        } else {
            // 填空/代码题：正确答案答对的学生 + 错误答案分布
            List<Map<String, Object>> correctStudents = new ArrayList<>();
            List<Map<String, Object>> wrongAnswers = new ArrayList<>();
            for (HomeworkSubmission s : subs) {
                Map<String, Object> ans = parseAnswers(s.getAnswersJson());
                String val = ans == null ? "" : String.valueOf(ans.getOrDefault(String.valueOf(index), "")).trim();
                if (val.isEmpty()) continue;
                answeredCount++;
                boolean right = matchesAnswer(type, val, correct);
                Map<String, Object> entry = new HashMap<>();
                entry.put("student", studentLabel(s));
                entry.put("answer", val);
                if (right) {
                    correctStudents.add(entry);
                    correctCount++;
                } else {
                    wrongAnswers.add(entry);
                }
            }
            m.put("correctStudents", correctStudents);
            m.put("wrongAnswers", wrongAnswers);
            m.put("correctCount", correctCount);
        }
        m.put("submitCount", subs.size());
        m.put("correctRate", subs.isEmpty() ? 0 : Math.round(correctCount * 1000.0 / subs.size()) / 10.0);
        return m;
    }

    private boolean matchesAnswer(String type, String student, String correct) {
        if ("blank".equals(type)) {
            String[] accepted = correct.split("\\|");
            for (String a : accepted) {
                if (student.replaceAll("\\s", "").equalsIgnoreCase(a.trim().replaceAll("\\s", ""))) return true;
            }
            return false;
        }
        return student.equalsIgnoreCase(correct);
    }

    private String studentLabel(HomeworkSubmission s) {
        User u = userMapper.selectById(s.getUserId());
        if (u == null) return String.valueOf(s.getUserId());
        return (u.getRealName() == null ? u.getUsername() : u.getRealName()) + "（" + u.getUsername() + "）";
    }

    private Map<String, Object> parseAnswers(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    /** 教师查看作业列表 */
    @GetMapping("/teacher/homework/{packId}")
    public R<List<Homework>> listForTeacher(@PathVariable Long packId) {
        AuthUtil.requireTeacher();
        return R.ok(homeworkMapper.selectList(
                new QueryWrapper<Homework>().eq("lesson_pack_id", packId).orderByAsc("id")));
    }

    @DeleteMapping("/teacher/homework/{id}")
    public R<Void> delete(@PathVariable Long id) {
        AuthUtil.requireTeacher();
        homeworkMapper.deleteById(id);
        submissionMapper.delete(new QueryWrapper<HomeworkSubmission>().eq("homework_id", id));
        return R.ok();
    }

    /** 教师打回单个学生的作业提交，允许其重做 */
    @DeleteMapping("/teacher/homework/submission/{submissionId}")
    public R<Void> reject(@PathVariable Long submissionId) {
        AuthUtil.requireTeacher();
        submissionMapper.deleteById(submissionId);
        return R.ok();
    }

    private int calcTotal(String questionsJson) {
        if (questionsJson == null || questionsJson.isBlank()) return 0;
        try {
            List<Map<String, Object>> qs = objectMapper.readValue(questionsJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            int total = 0;
            for (Map<String, Object> q : qs) {
                total += ((Number) q.getOrDefault("score", 0)).intValue();
            }
            return total;
        } catch (Exception e) {
            return 0;
        }
    }
}
