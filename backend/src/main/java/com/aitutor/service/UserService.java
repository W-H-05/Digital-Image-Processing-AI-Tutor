package com.aitutor.service;

import com.aitutor.entity.User;

import java.util.List;
import java.util.Map;

public interface UserService {

    /** 登录，返回 token */
    Map<String, Object> login(String username, String password);

    /** 修改密码 */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /** 教师批量导入学生（CSV: 学号,姓名,班级） */
    int importStudents(String csvContent);

    /** 重置学生密码为学号 */
    void resetPassword(Long studentId);

    /** 批量重置学生密码为学号 */
    int resetPasswordBatch(java.util.List<Long> studentIds);

    /** 学生详细学习记录 */
    Map<String, Object> studentDetail(Long studentId);

    /** 学生列表（教师端） */
    List<Map<String, Object>> listStudents();

    /** 根据 id 查询 */
    User getById(Long id);
}
