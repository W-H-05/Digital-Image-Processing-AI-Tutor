package com.aitutor.controller;

import com.aitutor.aspect.RateLimit;
import com.aitutor.common.R;
import com.aitutor.security.AuthUtil;
import com.aitutor.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 教师备课助手
 */
@RestController
@RequestMapping("/api/teacher/ai")
@RequiredArgsConstructor
public class TeacherAiController {

    private final AiService aiService;

    @RateLimit(key = "ai-prepare", limit = 20, windowSec = 3600)
    @PostMapping("/prepare")
    public R<Map<String, Object>> prepare(@RequestBody Map<String, String> body) {
        AuthUtil.requireTeacher();
        return R.ok(aiService.prepareLesson(body.get("teachingPlan")));
    }
}
