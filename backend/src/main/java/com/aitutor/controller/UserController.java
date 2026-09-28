package com.aitutor.controller;

import com.aitutor.common.R;
import com.aitutor.security.AuthUtil;
import com.aitutor.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 教师端学生管理
 */
@RestController
@RequestMapping("/api/teacher/students")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/import")
    public R<Map<String, Object>> importStudents(@RequestBody Map<String, String> body) {
        AuthUtil.requireTeacher();
        int count = userService.importStudents(body.get("csv"));
        return R.ok(Map.of("imported", count));
    }

    @PostMapping("/{id}/reset")
    public R<Void> resetPassword(@PathVariable Long id) {
        AuthUtil.requireTeacher();
        userService.resetPassword(id);
        return R.ok();
    }

    @PostMapping("/batch-reset")
    public R<Map<String, Object>> batchReset(@RequestBody Map<String, List<Long>> body) {
        AuthUtil.requireTeacher();
        int count = userService.resetPasswordBatch(body.get("ids"));
        return R.ok(Map.of("reset", count));
    }

    @GetMapping("/{id}/detail")
    public R<Map<String, Object>> studentDetail(@PathVariable Long id) {
        AuthUtil.requireTeacher();
        return R.ok(userService.studentDetail(id));
    }

    @GetMapping
    public R<List<Map<String, Object>>> listStudents() {
        AuthUtil.requireTeacher();
        return R.ok(userService.listStudents());
    }
}
