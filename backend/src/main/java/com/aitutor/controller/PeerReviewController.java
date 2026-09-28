package com.aitutor.controller;

import com.aitutor.common.R;
import com.aitutor.dto.PeerReviewDTO;
import com.aitutor.entity.PeerReview;
import com.aitutor.mapper.PeerReviewMapper;
import com.aitutor.security.AuthUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/peer-reviews")
@RequiredArgsConstructor
public class PeerReviewController {

    private final PeerReviewMapper peerReviewMapper;

    @PostMapping
    public R<PeerReview> submit(@RequestBody PeerReviewDTO dto) {
        Long reviewerId = AuthUtil.currentUserId();
        PeerReview pr = new PeerReview();
        pr.setReviewerId(reviewerId);
        pr.setRevieweeId(dto.getRevieweeId());
        pr.setTaskId(dto.getTaskId());
        pr.setScoresJson(dto.getScoresJson());
        pr.setComments(dto.getComments());
        peerReviewMapper.insert(pr);
        return R.ok(pr);
    }

    @GetMapping("/task/{taskId}")
    public R<List<PeerReview>> listByTask(@PathVariable Long taskId) {
        AuthUtil.currentUserId();
        return R.ok(peerReviewMapper.selectList(
                new QueryWrapper<PeerReview>().eq("task_id", taskId).orderByDesc("id")));
    }

    @GetMapping("/mine")
    public R<List<PeerReview>> mine() {
        Long userId = AuthUtil.currentUserId();
        return R.ok(peerReviewMapper.selectList(
                new QueryWrapper<PeerReview>().eq("reviewee_id", userId).orderByDesc("id")));
    }
}
