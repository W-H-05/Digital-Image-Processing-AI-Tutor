package com.aitutor.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 视图切换拦截器：读取 X-View-As 请求头，教师可切换学生视角。
 */
@Component
public class ViewContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String viewAs = request.getHeader("X-View-As");
        ViewContext.setStudentView("student".equalsIgnoreCase(viewAs));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ViewContext.clear();
    }
}
