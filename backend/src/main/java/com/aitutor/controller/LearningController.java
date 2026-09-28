package com.aitutor.controller;

import com.aitutor.common.R;
import com.aitutor.dto.OperationDTO;
import com.aitutor.security.AuthUtil;
import com.aitutor.service.LearningService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class LearningController {

    private final LearningService learningService;

    /** 记录操作埋点 */
    @PostMapping("/operation")
    public R<Void> recordOperation(@RequestBody OperationDTO dto) {
        Long userId = AuthUtil.currentUserId();
        learningService.recordOperation(userId, dto.getLessonPackId(), dto.getMaterialId(),
                dto.getActionType(), dto.getActionDetail(), dto.getDuration());
        return R.ok();
    }

    /** 学生个人学情统计 */
    @GetMapping("/self-stats")
    public R<java.util.Map<String, Object>> selfStats() {
        Long userId = AuthUtil.currentUserId();
        return R.ok(learningService.selfStats(userId));
    }

    /** 教师查看埋点操作记录（支持按课次/学生/类型筛选） */
    @GetMapping("/operations")
    public R<java.util.List<java.util.Map<String, Object>>> operations(
            @RequestParam(value = "lessonPackId", required = false) Long lessonPackId,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestParam(value = "actionType", required = false) String actionType,
            @RequestParam(value = "limit", defaultValue = "200") int limit) {
        AuthUtil.requireTeacher();
        return R.ok(learningService.listOperations(lessonPackId, studentId, actionType, limit));
    }
}
