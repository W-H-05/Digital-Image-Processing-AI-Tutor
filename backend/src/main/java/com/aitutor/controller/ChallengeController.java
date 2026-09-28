package com.aitutor.controller;

import com.aitutor.common.R;
import com.aitutor.dto.ChallengeDTO;
import com.aitutor.entity.ChallengeSubmission;
import com.aitutor.mapper.ChallengeSubmissionMapper;
import com.aitutor.security.AuthUtil;
import com.aitutor.service.LearningService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeSubmissionMapper challengeMapper;
    private final LearningService learningService;

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
    public R<List<ChallengeSubmission>> list(@PathVariable Long packId) {
        AuthUtil.currentUserId();
        return R.ok(challengeMapper.selectList(
                new QueryWrapper<ChallengeSubmission>().eq("lesson_pack_id", packId).orderByDesc("id")));
    }
}
