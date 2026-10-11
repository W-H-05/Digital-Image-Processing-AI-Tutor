package com.aitutor.controller;

import com.aitutor.common.R;
import com.aitutor.entity.PracticeQuestion;
import com.aitutor.entity.PracticeRecord;
import com.aitutor.mapper.PracticeQuestionMapper;
import com.aitutor.mapper.PracticeRecordMapper;
import com.aitutor.security.AuthUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 练习题系统：教师管理选择题（单选/多选），学生即时反馈练习
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeQuestionMapper questionMapper;
    private final PracticeRecordMapper recordMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 教师端题目管理 ====================

    /** 教师查看某课次的练习题列表（含答案） */
    @GetMapping("/teacher/practice/{packId}")
    public R<List<Map<String, Object>>> listForTeacher(@PathVariable Long packId) {
        AuthUtil.requireTeacher();
        List<PracticeQuestion> list = questionMapper.selectList(
                new QueryWrapper<PracticeQuestion>().eq("lesson_pack_id", packId).orderByAsc("question_no"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (PracticeQuestion q : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", q.getId());
            m.put("lessonPackId", q.getLessonPackId());
            m.put("questionNo", q.getQuestionNo());
            m.put("stem", q.getStem());
            m.put("options", parseOptions(q.getOptionsJson()));
            m.put("correctKeys", parseKeys(q.getCorrectKeys()));
            m.put("isMultiple", q.getIsMultiple());
            result.add(m);
        }
        return R.ok(result);
    }

    /** 教师新增题目（自动题号） */
    @PostMapping("/teacher/practice/{packId}")
    public R<PracticeQuestion> add(@PathVariable Long packId, @RequestBody Map<String, Object> body) {
        AuthUtil.requireTeacher();
        PracticeQuestion q = new PracticeQuestion();
        q.setLessonPackId(packId);
        // 自动题号 = 当前最大题号 + 1
        PracticeQuestion last = questionMapper.selectOne(
                new QueryWrapper<PracticeQuestion>().eq("lesson_pack_id", packId).orderByDesc("question_no").last("limit 1"));
        q.setQuestionNo(last == null || last.getQuestionNo() == null ? 1 : last.getQuestionNo() + 1);
        q.setStem(String.valueOf(body.getOrDefault("stem", "")));
        List<?> options = (List<?>) body.getOrDefault("options", new ArrayList<>());
        q.setOptionsJson(writeJson(options));
        List<?> keys = (List<?>) body.getOrDefault("correctKeys", new ArrayList<>());
        q.setCorrectKeys(String.join(",", keys.stream().map(String::valueOf).toList()));
        q.setIsMultiple(Boolean.TRUE.equals(body.get("isMultiple")) ? 1 : 0);
        questionMapper.insert(q);
        return R.ok(q);
    }

    /** 教师更新题目 */
    @PutMapping("/teacher/practice/{id}")
    public R<PracticeQuestion> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        AuthUtil.requireTeacher();
        PracticeQuestion q = questionMapper.selectById(id);
        if (q == null) return R.fail(404, "题目不存在");
        if (body.containsKey("stem")) q.setStem(String.valueOf(body.get("stem")));
        if (body.containsKey("options")) q.setOptionsJson(writeJson((List<?>) body.get("options")));
        if (body.containsKey("correctKeys")) {
            List<?> keys = (List<?>) body.get("correctKeys");
            q.setCorrectKeys(String.join(",", keys.stream().map(String::valueOf).toList()));
        }
        if (body.containsKey("isMultiple")) q.setIsMultiple(Boolean.TRUE.equals(body.get("isMultiple")) ? 1 : 0);
        questionMapper.updateById(q);
        return R.ok(q);
    }

    /** 教师删除题目（删除后重排题号） */
    @DeleteMapping("/teacher/practice/{id}")
    public R<Void> delete(@PathVariable Long id) {
        AuthUtil.requireTeacher();
        PracticeQuestion q = questionMapper.selectById(id);
        if (q == null) return R.fail(404, "题目不存在");
        Long packId = q.getLessonPackId();
        questionMapper.deleteById(id);
        // 重排题号
        List<PracticeQuestion> list = questionMapper.selectList(
                new QueryWrapper<PracticeQuestion>().eq("lesson_pack_id", packId).orderByAsc("question_no"));
        int no = 1;
        for (PracticeQuestion qq : list) {
            qq.setQuestionNo(no++);
            questionMapper.updateById(qq);
        }
        // 同时删除该题作答记录
        recordMapper.delete(new QueryWrapper<PracticeRecord>().eq("question_id", id));
        return R.ok();
    }

    // ==================== 学生端练习 ====================

    /** 学生获取某课次练习题（不含答案） */
    @GetMapping("/lesson-packs/{packId}/practice")
    public R<List<Map<String, Object>>> listForStudent(@PathVariable Long packId) {
        AuthUtil.currentUserId();
        List<PracticeQuestion> list = questionMapper.selectList(
                new QueryWrapper<PracticeQuestion>().eq("lesson_pack_id", packId).orderByAsc("question_no"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (PracticeQuestion q : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", q.getId());
            m.put("questionNo", q.getQuestionNo());
            m.put("stem", q.getStem());
            m.put("options", parseOptions(q.getOptionsJson()));
            m.put("isMultiple", q.getIsMultiple());
            result.add(m);
        }
        return R.ok(result);
    }

    /** 学生提交单题作答，返回是否正确 */
    @PostMapping("/practice/{id}/answer")
    public R<Map<String, Object>> answer(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = AuthUtil.currentUserId();
        PracticeQuestion q = questionMapper.selectById(id);
        if (q == null) return R.fail(404, "题目不存在");
        List<?> chosen = (List<?>) body.getOrDefault("keys", new ArrayList<>());
        Set<String> chosenSet = new TreeSet<>(chosen.stream().map(String::valueOf).map(String::toUpperCase).toList());
        Set<String> correctSet = new TreeSet<>(parseKeys(q.getCorrectKeys()));
        boolean correct = chosenSet.equals(correctSet);

        Map<String, Object> result = new HashMap<>();
        result.put("correct", correct);
        result.put("correctKeys", new ArrayList<>(correctSet));
        return R.ok(result);
    }

    /** 学生记录一次作答（供学情分析：正确/错误、错因） */
    @PostMapping("/practice/{id}/record")
    public R<Void> record(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = AuthUtil.currentUserId();
        PracticeQuestion q = questionMapper.selectById(id);
        if (q == null) return R.fail(404, "题目不存在");
        PracticeRecord r = new PracticeRecord();
        r.setUserId(userId);
        r.setLessonPackId(q.getLessonPackId());
        r.setQuestionId(id);
        r.setIsCorrect(Boolean.TRUE.equals(body.get("correct")) ? 1 : 0);
        r.setWrongCount(body.get("wrongCount") == null ? 0 : ((Number) body.get("wrongCount")).intValue());
        r.setWrongReason(body.get("wrongReason") == null ? "" : String.valueOf(body.get("wrongReason")));
        recordMapper.insert(r);
        return R.ok();
    }

    // ==================== 学情分析 ====================

    /** 教师查看练习学情：正确率、每题错误率、错因汇总 */
    @GetMapping("/teacher/practice/{packId}/stats")
    public R<Map<String, Object>> stats(@PathVariable Long packId) {
        AuthUtil.requireTeacher();
        List<PracticeQuestion> questions = questionMapper.selectList(
                new QueryWrapper<PracticeQuestion>().eq("lesson_pack_id", packId).orderByAsc("question_no"));
        List<PracticeRecord> records = recordMapper.selectList(
                new QueryWrapper<PracticeRecord>().eq("lesson_pack_id", packId));

        int totalCorrect = 0, totalWrong = 0;
        for (PracticeRecord r : records) {
            if (r.getIsCorrect() != null && r.getIsCorrect() == 1) totalCorrect++;
            else totalWrong++;
        }
        // 每题正确率
        List<Map<String, Object>> perQuestion = new ArrayList<>();
        for (PracticeQuestion q : questions) {
            int c = 0, w = 0;
            for (PracticeRecord r : records) {
                if (r.getQuestionId().equals(q.getId())) {
                    if (r.getIsCorrect() != null && r.getIsCorrect() == 1) c++; else w++;
                }
            }
            Map<String, Object> m = new HashMap<>();
            m.put("questionId", q.getId());
            m.put("questionNo", q.getQuestionNo());
            m.put("stem", q.getStem());
            m.put("correctCount", c);
            m.put("wrongCount", w);
            m.put("accuracy", (c + w) == 0 ? 0 : Math.round(c * 1000.0 / (c + w)) / 10.0);
            perQuestion.add(m);
        }
        // 错因汇总
        Map<String, Integer> reasonCounter = new HashMap<>();
        for (PracticeRecord r : records) {
            if (r.getWrongReason() != null && !r.getWrongReason().isBlank()) {
                reasonCounter.merge(r.getWrongReason().trim(), 1, Integer::sum);
            }
        }
        List<Map<String, Object>> reasons = reasonCounter.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> { Map<String, Object> m = new HashMap<>(); m.put("reason", e.getKey()); m.put("count", e.getValue()); return m; })
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("questionCount", questions.size());
        result.put("totalCorrect", totalCorrect);
        result.put("totalWrong", totalWrong);
        result.put("totalAccuracy", (totalCorrect + totalWrong) == 0 ? 0 : Math.round(totalCorrect * 1000.0 / (totalCorrect + totalWrong)) / 10.0);
        result.put("perQuestion", perQuestion);
        result.put("wrongReasons", reasons);
        return R.ok(result);
    }

    // ==================== 工具方法 ====================

    private List<Map<String, Object>> parseOptions(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<String> parseKeys(String keys) {
        if (keys == null || keys.isBlank()) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (String k : keys.split(",")) {
            String kk = k.trim().toUpperCase();
            if (!kk.isEmpty()) result.add(kk);
        }
        return result;
    }

    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }
}
