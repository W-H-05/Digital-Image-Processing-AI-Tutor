package com.aitutor.controller;

import com.aitutor.common.R;
import com.aitutor.dto.ChangePasswordDTO;
import com.aitutor.dto.LoginDTO;
import com.aitutor.entity.User;
import com.aitutor.security.AuthUtil;
import com.aitutor.service.UserService;
import com.aitutor.service.OnlineService;
import com.aitutor.service.LearningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final OnlineService onlineService;
    private final LearningService learningService;

    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        Map<String, Object> result = userService.login(dto.getUsername(), dto.getPassword());
        // 记录登录埋点
        Object uid = result.get("userId");
        if (uid != null) {
            learningService.recordOperation(Long.valueOf(uid.toString()), null, null,
                    "login", "用户登录", 0);
        }
        return R.ok(result);
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        Long userId = AuthUtil.currentUserId();
        // 记录退出埋点
        learningService.recordOperation(userId, null, null, "logout", "用户退出", 0);
        onlineService.remove(userId);
        cn.dev33.satoken.stp.StpUtil.logout();
        return R.ok();
    }

    @PostMapping("/change-password")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(AuthUtil.currentUserId(), dto.getOldPassword(), dto.getNewPassword());
        return R.ok();
    }

    /** 获取当前登录用户信息 */
    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        Long userId = AuthUtil.currentUserId();
        User user = userService.getById(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("role", user.getRole());
        result.put("className", user.getClassName());
        return R.ok(result);
    }
}
