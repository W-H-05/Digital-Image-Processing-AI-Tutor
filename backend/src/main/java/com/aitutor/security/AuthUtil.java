package com.aitutor.security;

import cn.dev33.satoken.stp.StpUtil;
import com.aitutor.common.BizException;
import com.aitutor.common.Constants;

/**
 * 当前登录用户工具
 */
public class AuthUtil {

    public static Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    public static boolean isTeacher() {
        return StpUtil.hasRole(Constants.ROLE_TEACHER);
    }

    public static boolean isStudent() {
        return StpUtil.hasRole(Constants.ROLE_STUDENT);
    }

    /**
     * 判断当前请求是否按"教师视角"处理。
     * 教师可携带 X-View-As: student 切换到学生视角，此时视为学生（返回 false）。
     */
    public static boolean effectiveTeacher() {
        if (!isTeacher()) return false;
        return !ViewContext.isStudentView();
    }

    /** 要求教师角色，否则抛异常 */
    public static void requireTeacher() {
        if (!isTeacher()) {
            throw new BizException(403, "无权限：仅教师可操作");
        }
    }

    /** 要求学生角色，否则抛异常 */
    public static void requireStudent() {
        if (!isStudent()) {
            throw new BizException(403, "无权限：仅学生可操作");
        }
    }
}
