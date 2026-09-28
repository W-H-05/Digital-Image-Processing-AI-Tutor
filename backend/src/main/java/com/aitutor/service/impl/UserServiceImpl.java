package com.aitutor.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.aitutor.common.BizException;
import com.aitutor.common.Constants;
import com.aitutor.entity.*;
import com.aitutor.mapper.*;
import com.aitutor.service.UserService;
import com.aitutor.service.OnlineService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final QaRecordMapper qaRecordMapper;
    private final StudentOperationMapper studentOperationMapper;
    private final ChallengeSubmissionMapper challengeSubmissionMapper;
    private final PeerReviewMapper peerReviewMapper;
    private final StudentMaterialViewMapper studentMaterialViewMapper;
    private final OnlineService onlineService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Map<String, Object> login(String username, String password) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException(400, "账号或密码错误");
        }
        StpUtil.login(user.getId());
        StpUtil.getSession().set("role", user.getRole());
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
        // 记录在线
        onlineService.touch(user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("token", StpUtil.getTokenValue());
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("role", user.getRole());
        result.put("className", user.getClassName());
        return result;
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BizException(404, "用户不存在");
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BizException(400, "原密码错误");
        }
        if (newPassword.length() < 4 || newPassword.length() > 32) {
            throw new BizException(400, "密码长度需在 4-32 位之间");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    @Override
    public int importStudents(String csvContent) {
        if (csvContent == null || csvContent.isBlank()) {
            throw new BizException(400, "CSV 内容为空");
        }
        String[] lines = csvContent.split("\\r?\\n");
        int count = 0;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(",");
            if (parts.length < 2) continue;
            String username = parts[0].trim();
            String realName = parts[1].trim();
            String className = parts.length > 2 ? parts[2].trim() : "";
            if (username.isEmpty()) continue;
            User exist = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
            if (exist != null) {
                exist.setRealName(realName);
                exist.setClassName(className);
                userMapper.updateById(exist);
                continue;
            }
            User student = new User();
            student.setUsername(username);
            student.setPasswordHash(passwordEncoder.encode(username));
            student.setRole(Constants.ROLE_STUDENT);
            student.setRealName(realName);
            student.setClassName(className);
            userMapper.insert(student);
            count++;
        }
        return count;
    }

    @Override
    public void resetPassword(Long studentId) {
        User user = userMapper.selectById(studentId);
        if (user == null) throw new BizException(404, "学生不存在");
        if (!Constants.ROLE_STUDENT.equals(user.getRole())) {
            throw new BizException(400, "仅可重置学生密码");
        }
        user.setPasswordHash(passwordEncoder.encode(user.getUsername()));
        userMapper.updateById(user);
    }

    @Override
    public int resetPasswordBatch(List<Long> studentIds) {
        int count = 0;
        for (Long id : studentIds) {
            try {
                resetPassword(id);
                count++;
            } catch (Exception ignored) {
            }
        }
        return count;
    }

    @Override
    public Map<String, Object> studentDetail(Long studentId) {
        User user = userMapper.selectById(studentId);
        if (user == null) throw new BizException(404, "学生不存在");
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("className", user.getClassName());
        result.put("lastLoginTime", user.getLastLoginTime());

        // 问答记录
        List<QaRecord> qas = qaRecordMapper.selectList(
                new QueryWrapper<QaRecord>().eq("user_id", studentId).orderByDesc("id").last("limit 50"));
        result.put("qaRecords", qas);
        result.put("questionCount", qas.size());

        // 操作记录
        List<StudentOperation> ops = studentOperationMapper.selectList(
                new QueryWrapper<StudentOperation>().eq("user_id", studentId).orderByDesc("id").last("limit 50"));
        result.put("operations", ops);
        result.put("operationCount", ops.size());

        // 挑战提交
        List<ChallengeSubmission> challenges = challengeSubmissionMapper.selectList(
                new QueryWrapper<ChallengeSubmission>().eq("user_id", studentId).orderByDesc("id").last("limit 20"));
        result.put("challenges", challenges);
        result.put("challengeCount", challenges.size());

        // 互评
        List<PeerReview> reviews = peerReviewMapper.selectList(
                new QueryWrapper<PeerReview>().eq("reviewer_id", studentId).orderByDesc("id").last("limit 20"));
        result.put("peerReviews", reviews);
        result.put("peerReviewCount", reviews.size());

        // 材料阅览记录
        List<StudentMaterialView> views = studentMaterialViewMapper.selectList(
                new QueryWrapper<StudentMaterialView>().eq("user_id", studentId).orderByDesc("id").last("limit 50"));
        result.put("materialViews", views);
        result.put("materialViewCount", views.size());

        return result;
    }

    @Override
    public List<Map<String, Object>> listStudents() {
        List<User> students = userMapper.selectList(
                new QueryWrapper<User>().eq("role", Constants.ROLE_STUDENT).orderByAsc("username"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (User s : students) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("username", s.getUsername());
            m.put("realName", s.getRealName());
            m.put("className", s.getClassName());
            m.put("lastLoginTime", s.getLastLoginTime());
            Long questionCount = qaRecordMapper.selectCount(
                    new QueryWrapper<QaRecord>().eq("user_id", s.getId()));
            Long operationCount = studentOperationMapper.selectCount(
                    new QueryWrapper<StudentOperation>().eq("user_id", s.getId()));
            m.put("questionCount", questionCount);
            m.put("operationCount", operationCount);
            result.add(m);
        }
        return result;
    }

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }
}
