package com.aitutor.controller;

import com.aitutor.common.R;
import com.aitutor.entity.*;
import com.aitutor.mapper.*;
import com.aitutor.security.AuthUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 教师端互评答辩管理：量表、活动（模式+计时）、统计
 */
@RestController
@RequestMapping("/api/teacher/review")
@RequiredArgsConstructor
public class PeerReviewManageController {

    private final PeerReviewRubricMapper rubricMapper;
    private final PeerReviewActivityMapper activityMapper;
    private final PeerReviewMapper reviewMapper;
    private final UserMapper userMapper;

    // ==================== 互评量表 ====================

    @GetMapping("/rubrics")
    public R<List<PeerReviewRubric>> rubrics() {
        AuthUtil.requireTeacher();
        return R.ok(rubricMapper.selectList(
                new QueryWrapper<PeerReviewRubric>().orderByAsc("id")));
    }

    @PostMapping("/rubrics")
    public R<PeerReviewRubric> addRubric(@RequestBody PeerReviewRubric rubric) {
        AuthUtil.requireTeacher();
        if (rubric.getName() == null || rubric.getName().isBlank()) {
            return R.fail(400, "量表名称不能为空");
        }
        if (rubric.getDimensionsJson() == null || rubric.getDimensionsJson().isBlank()) {
            return R.fail(400, "请配置评分维度");
        }
        rubricMapper.insert(rubric);
        return R.ok(rubric);
    }

    @PutMapping("/rubrics/{id}")
    public R<PeerReviewRubric> updateRubric(@PathVariable Long id, @RequestBody PeerReviewRubric rubric) {
        AuthUtil.requireTeacher();
        PeerReviewRubric exist = rubricMapper.selectById(id);
        if (exist == null) return R.fail(404, "量表不存在");
        rubric.setId(id);
        rubricMapper.updateById(rubric);
        return R.ok(rubricMapper.selectById(id));
    }

    @DeleteMapping("/rubrics/{id}")
    public R<Void> deleteRubric(@PathVariable Long id) {
        AuthUtil.requireTeacher();
        rubricMapper.deleteById(id);
        return R.ok();
    }

    // ==================== 互评活动 ====================

    @GetMapping("/activities")
    public R<List<PeerReviewActivity>> activities() {
        AuthUtil.requireTeacher();
        return R.ok(activityMapper.selectList(
                new QueryWrapper<PeerReviewActivity>().orderByDesc("id")));
    }

    @PostMapping("/activities")
    public R<PeerReviewActivity> addActivity(@RequestBody PeerReviewActivity activity) {
        AuthUtil.requireTeacher();
        if (activity.getName() == null || activity.getName().isBlank()) {
            return R.fail(400, "活动名称不能为空");
        }
        if (activity.getStatus() == null) activity.setStatus("PENDING");
        activityMapper.insert(activity);
        return R.ok(activity);
    }

    @PutMapping("/activities/{id}")
    public R<PeerReviewActivity> updateActivity(@PathVariable Long id, @RequestBody PeerReviewActivity activity) {
        AuthUtil.requireTeacher();
        PeerReviewActivity exist = activityMapper.selectById(id);
        if (exist == null) return R.fail(404, "活动不存在");
        activity.setId(id);
        activityMapper.updateById(activity);
        return R.ok(activityMapper.selectById(id));
    }

    /** 开始活动（启动计时） */
    @PostMapping("/activities/{id}/start")
    public R<PeerReviewActivity> startActivity(@PathVariable Long id) {
        AuthUtil.requireTeacher();
        PeerReviewActivity a = activityMapper.selectById(id);
        if (a == null) return R.fail(404, "活动不存在");
        a.setStatus("RUNNING");
        a.setStartTime(LocalDateTime.now());
        a.setEndTime(LocalDateTime.now().plusSeconds(a.getDurationSec() == null ? 300 : a.getDurationSec()));
        activityMapper.updateById(a);
        return R.ok(a);
    }

    /** 结束活动 */
    @PostMapping("/activities/{id}/finish")
    public R<PeerReviewActivity> finishActivity(@PathVariable Long id) {
        AuthUtil.requireTeacher();
        PeerReviewActivity a = activityMapper.selectById(id);
        if (a == null) return R.fail(404, "活动不存在");
        a.setStatus("FINISHED");
        a.setEndTime(LocalDateTime.now());
        activityMapper.updateById(a);
        return R.ok(a);
    }

    @DeleteMapping("/activities/{id}")
    public R<Void> deleteActivity(@PathVariable Long id) {
        AuthUtil.requireTeacher();
        activityMapper.deleteById(id);
        return R.ok();
    }

    // ==================== 统计 ====================

    /** 互评统计：各学生的平均分、评分维度平均分、评论列表 */
    @GetMapping("/stats")
    public R<Map<String, Object>> stats(@RequestParam(value = "activityId", required = false) Long activityId) {
        AuthUtil.requireTeacher();
        List<PeerReview> reviews = reviewMapper.selectList(
                new QueryWrapper<PeerReview>().eq(activityId != null, "task_id", activityId));
        Map<String, Object> result = new HashMap<>();
        result.put("totalReviews", reviews.size());

        // 按被评人聚合
        Map<Long, List<PeerReview>> byReviewee = new HashMap<>();
        for (PeerReview r : reviews) {
            byReviewee.computeIfAbsent(r.getRevieweeId(), k -> new ArrayList<>()).add(r);
        }

        List<Map<String, Object>> studentStats = new ArrayList<>();
        for (Map.Entry<Long, List<PeerReview>> e : byReviewee.entrySet()) {
            User u = userMapper.selectById(e.getKey());
            List<PeerReview> list = e.getValue();
            Map<String, Object> m = new HashMap<>();
            m.put("studentId", e.getKey());
            m.put("studentName", u == null ? "未知" : (u.getRealName() == null ? u.getUsername() : u.getRealName()));
            m.put("username", u == null ? "" : u.getUsername());
            m.put("reviewCount", list.size());
            m.put("avgScore", calcAvg(list));
            // 评论列表
            List<String> comments = new ArrayList<>();
            for (PeerReview r : list) {
                if (r.getComments() != null && !r.getComments().isBlank()) {
                    comments.add(r.getComments());
                }
            }
            m.put("comments", comments);
            studentStats.add(m);
        }
        result.put("students", studentStats);
        return R.ok(result);
    }

    private double calcAvg(List<PeerReview> list) {
        if (list.isEmpty()) return 0;
        double sum = 0;
        int count = 0;
        for (PeerReview r : list) {
            if (r.getScoresJson() == null) continue;
            try {
                Map<String, Object> scores = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                        r.getScoresJson(), Map.class);
                for (Object v : scores.values()) {
                    sum += Double.parseDouble(String.valueOf(v));
                    count++;
                }
            } catch (Exception ignored) {
            }
        }
        return count == 0 ? 0 : Math.round(sum / count * 10) / 10.0;
    }
}
