package com.aitutor.controller;

import com.aitutor.common.R;
import com.aitutor.security.AuthUtil;
import com.aitutor.service.LearningService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}
