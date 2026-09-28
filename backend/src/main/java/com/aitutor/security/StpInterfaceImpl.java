package com.aitutor.security;

import cn.dev33.satoken.stp.StpInterface;
import com.aitutor.entity.User;
import com.aitutor.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限扩展：根据登录用户查询角色列表
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserMapper userMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<String> roles = new ArrayList<>();
        try {
            User user = userMapper.selectById(Long.valueOf(loginId.toString()));
            if (user != null && user.getRole() != null) {
                roles.add(user.getRole());
            }
        } catch (Exception ignored) {
        }
        return roles;
    }
}
