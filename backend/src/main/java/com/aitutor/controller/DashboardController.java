package com.aitutor.controller;

import com.aitutor.common.R;
import com.aitutor.entity.PracticeQuestion;
import com.aitutor.entity.PracticeRecord;
import com.aitutor.mapper.PracticeQuestionMapper;
import com.aitutor.mapper.PracticeRecordMapper;
import com.aitutor.security.AuthUtil;
import com.aitutor.service.LearningService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学情看板（教师端）
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final LearningService learningService;
    private final PracticeQuestionMapper practiceQuestionMapper;
    private final PracticeRecordMapper practiceRecordMapper;

    @GetMapping("/overview")
    public R<Map<String, Object>> overview(@RequestParam(value = "lessonPackId", required = false) Long lessonPackId) {
        AuthUtil.requireTeacher();
        return R.ok(learningService.overview(lessonPackId));
    }

    @GetMapping("/top-confusion")
    public R<List<Map<String, Object>>> topConfusion(@RequestParam(value = "lessonPackId", required = false) Long lessonPackId) {
        AuthUtil.requireTeacher();
        return R.ok(learningService.topConfusion(lessonPackId));
    }

    @GetMapping("/mastery")
    public R<List<Map<String, Object>>> mastery(@RequestParam(value = "lessonPackId", required = false) Long lessonPackId) {
        AuthUtil.requireTeacher();
        return R.ok(learningService.mastery(lessonPackId));
    }

    @GetMapping("/stream")
    public R<List<Map<String, Object>>> stream(@RequestParam(value = "limit", defaultValue = "20") int limit) {
        AuthUtil.requireTeacher();
        return R.ok(learningService.stream(limit));
    }

    @GetMapping("/error-ranking")
    public R<List<Map<String, Object>>> errorRanking(@RequestParam(value = "lessonPackId", required = false) Long lessonPackId) {
        AuthUtil.requireTeacher();
        return R.ok(learningService.errorRanking(lessonPackId));
    }

    @GetMapping("/anomalies")
    public R<List<Map<String, Object>>> anomalies(@RequestParam(value = "lessonPackId", required = false) Long lessonPackId) {
        AuthUtil.requireTeacher();
        return R.ok(learningService.anomalies(lessonPackId));
    }

    @GetMapping("/report")
    public R<Map<String, Object>> report(@RequestParam(value = "lessonPackId", required = false) Long lessonPackId) {
        AuthUtil.requireTeacher();
        return R.ok(learningService.lessonReport(lessonPackId));
    }

    @GetMapping("/word-cloud")
    public R<List<Map<String, Object>>> wordCloud(@RequestParam(value = "lessonPackId", required = false) Long lessonPackId,
                                                   @RequestParam(value = "limit", defaultValue = "50") int limit) {
        AuthUtil.requireTeacher();
        return R.ok(learningService.wordCloud(lessonPackId, limit));
    }

    /** 练习学情：正确率、每题正确率、错因汇总（支持课次过滤） */
    @GetMapping("/practice")
    public R<Map<String, Object>> practiceStats(@RequestParam(value = "lessonPackId", required = false) Long lessonPackId) {
        AuthUtil.requireTeacher();
        List<PracticeQuestion> questions = practiceQuestionMapper.selectList(
                new QueryWrapper<PracticeQuestion>().eq(lessonPackId != null, "lesson_pack_id", lessonPackId).orderByAsc("question_no"));
        List<PracticeRecord> records = practiceRecordMapper.selectList(
                new QueryWrapper<PracticeRecord>().eq(lessonPackId != null, "lesson_pack_id", lessonPackId));

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
}
