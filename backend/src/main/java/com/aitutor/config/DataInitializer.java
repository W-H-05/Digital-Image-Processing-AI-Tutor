package com.aitutor.config;

import com.aitutor.common.Constants;
import com.aitutor.entity.User;
import com.aitutor.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化：创建默认教师账号（若不存在）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final AppProperties appProperties;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        try {
            User teacher = userMapper.selectOne(new QueryWrapper<User>().eq("username", appProperties.getTeacherUsername()));
            if (teacher == null) {
                teacher = new User();
                teacher.setUsername(appProperties.getTeacherUsername());
                teacher.setPasswordHash(passwordEncoder.encode(appProperties.getTeacherPassword()));
                teacher.setRole(Constants.ROLE_TEACHER);
                teacher.setRealName("授课教师");
                userMapper.insert(teacher);
                log.info("已创建默认教师账号：{}", appProperties.getTeacherUsername());
            }
        } catch (Exception e) {
            log.warn("教师账号初始化失败（可能数据库表未初始化，请先执行 sql 脚本）", e);
        }
    }
}
