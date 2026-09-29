package com.aitutor.controller;

import com.aitutor.common.R;
import com.aitutor.dto.ChallengeDTO;
import com.aitutor.entity.ChallengeSubmission;
import com.aitutor.entity.User;
import com.aitutor.mapper.ChallengeSubmissionMapper;
import com.aitutor.mapper.UserMapper;
import com.aitutor.security.AuthUtil;
import com.aitutor.service.LearningService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeSubmissionMapper challengeMapper;
    private final LearningService learningService;
    private final UserMapper userMapper;

    @PostMapping
    public R<ChallengeSubmission> submit(@RequestBody ChallengeDTO dto) {
        Long userId = AuthUtil.currentUserId();
        ChallengeSubmission cs = new ChallengeSubmission();
        cs.setUserId(userId);
        cs.setLessonPackId(dto.getLessonPackId());
        cs.setCoursewareId(dto.getCoursewareId());
        cs.setParamsJson(dto.getParamsJson());
        cs.setScreenshotPath(dto.getScreenshotPath());
        cs.setConclusion(dto.getConclusion());
        challengeMapper.insert(cs);

        learningService.recordOperation(userId, dto.getLessonPackId(), null,
                "challenge", "提交挑战结论", 0);
        return R.ok(cs);
    }

    @GetMapping("/{packId}")
    public R<List<Map<String, Object>>> list(@PathVariable Long packId) {
        Long currentUserId = AuthUtil.currentUserId();
        // 排除自己的挑战（互评他人作品）
        List<ChallengeSubmission> list = challengeMapper.selectList(
                new QueryWrapper<ChallengeSubmission>().eq("lesson_pack_id", packId)
                        .ne("user_id", currentUserId).orderByDesc("id"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChallengeSubmission c : list) {
            User u = userMapper.selectById(c.getUserId());
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("userId", c.getUserId());
            m.put("userName", u == null ? "未知" : (u.getRealName() == null ? u.getUsername() : u.getRealName()));
            m.put("username", u == null ? "" : u.getUsername());
            m.put("lessonPackId", c.getLessonPackId());
            m.put("coursewareId", c.getCoursewareId());
            m.put("paramsJson", c.getParamsJson());
            m.put("conclusion", c.getConclusion());
            m.put("createTime", c.getCreateTime());
            result.add(m);
        }
        return R.ok(result);
    }
}
