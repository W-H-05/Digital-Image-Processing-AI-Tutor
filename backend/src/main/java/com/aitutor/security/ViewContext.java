package com.aitutor.security;

/**
 * 请求级学生视角上下文：教师通过 header X-View-As: student 切换到学生视角查看。
 */
public class ViewContext {

    private static final ThreadLocal<Boolean> STUDENT_VIEW = ThreadLocal.withInitial(() -> false);

    public static void setStudentView(boolean v) {
        STUDENT_VIEW.set(v);
    }

    public static boolean isStudentView() {
        return STUDENT_VIEW.get();
    }

    public static void clear() {
        STUDENT_VIEW.remove();
    }
}
