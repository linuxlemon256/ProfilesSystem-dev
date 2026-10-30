package com.example.profilessystem.auth;

import com.example.profilessystem.mapper.ManagerMapper;
import com.example.profilessystem.mapper.UserMapper;
import com.example.profilessystem.mapper.FeatureMapper;
import com.example.profilessystem.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 登录校验：
 * 管理接口与 managerPanel.html 需要管理员会话；
 * learnServer.html 与其余 /api/user/** 需要用户会话。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final ManagerMapper managerMapper;
    private final UserMapper userMapper;
    private final FeatureMapper featureMapper;

    public AuthInterceptor(ManagerMapper managerMapper, UserMapper userMapper, FeatureMapper featureMapper) {
        this.managerMapper = managerMapper;
        this.userMapper = userMapper;
        this.featureMapper = featureMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String path = request.getRequestURI();

        // 管理员登录页需要读取初始化状态；初始化接口仅在管理员表为空时开放。
        if ("/api/manager/bootstrap/status".equals(path)) {
            return true;
        }
        if ("/api/manager/bootstrap".equals(path)
                && "POST".equalsIgnoreCase(request.getMethod())
                && managerMapper.count() == 0) {
            return true;
        }

        if (isManagerOnly(path)) {
            // 引导：系统里还没有任何管理员时，允许创建第一个管理员
            if (managerMapper.count() == 0
                    && "POST".equalsIgnoreCase(request.getMethod())
                    && ("/api/manager/insert/".equals(path)
                        || "/api/manager/insert".equals(path))) {
                return true;
            }
            if (sessionValue(request, SessionKeys.MANAGER) != null) {
                return true;
            }
            return reject(request, response, "/managerLogin.html");
        }

        String feature = featureFor(path);
        if (feature != null && !featureEnabled(feature)) {
            return rejectFeature(request, response, feature);
        }

        if (requiresUser(path) && sessionValue(request, SessionKeys.USER) == null) {
            return reject(request, response, "/login.html");
        }
        if (requiresAssociatedLevel(path)) {
            String username = sessionValue(request, SessionKeys.USER);
            if (username == null) {
                return reject(request, response, "/login.html");
            }
            var user = userMapper.findByName(username);
            if (user == null || user.getLevel() < requiredLevel(path)) {
                return rejectForbidden(request, response,
                        "/accessDenied.html?required=" + requiredLevel(path));
            }
        }
        return true;
    }

    private String sessionValue(HttpServletRequest request, String key) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(key);
        return value == null ? null : value.toString();
    }

    private boolean isManagerOnly(String path) {
        if (path.startsWith("/api/manager/") || path.equals("/managerPanel.html")) {
            return true;
        }
        return path.startsWith("/api/recovery/admin/")
                || path.startsWith("/api/community/admin/")
                || path.startsWith("/api/groups/admin/")
                || path.equals("/api/notifications/admin/send")
                || path.startsWith("/api/llm/admin/")
                || path.startsWith("/api/features/")
                || path.equals("/api/user/get/all")
                || path.equals("/api/user/get/uncommit")
                || path.equals("/api/user/get/commit")
                || path.equals("/api/user/get/rejected")
                || path.equals("/api/user/insert")
                || path.startsWith("/api/user/approve/")
                || path.startsWith("/api/user/reject/")
                || path.startsWith("/api/user/admin/");
    }

    private boolean requiresUser(String path) {
        // 学习路线页面公开；云端存储（进度同步）仍需登录
        return path.startsWith("/api/user/")
                || path.startsWith("/api/progress")
                || path.equals("/api/community/messages")
                || path.matches("/api/community/[0-9]+/messages")
                || path.startsWith("/api/groups")
                || path.startsWith("/api/social")
                || path.startsWith("/api/notifications")
                || path.startsWith("/api/llm");
    }

    private String featureFor(String path) {
        if (path.startsWith("/api/llm") || path.equals("/LLMindex.html")) return "llm";
        if (path.startsWith("/api/groups") || path.equals("/groups.html")) return "groups";
        if (path.startsWith("/api/social") || path.equals("/communication.html")) return "friends";
        if (path.startsWith("/api/notifications")) return "notifications";
        if (path.startsWith("/api/community")) return "community";
        if (path.equals("/notification.html")) return "notifications";
        if (path.equals("/feedback.html") || path.equals("/joinUs.html")) return "community";
        if (path.startsWith("/api/progress") || path.equals("/storageDemo.html")) return "storage";
        return null;
    }

    private boolean featureEnabled(String feature) {
        Integer enabled = featureMapper.enabled(feature);
        return enabled != null && enabled == 1;
    }

    private boolean rejectFeature(HttpServletRequest request, HttpServletResponse response, String feature) throws IOException {
        String reason = featureMapper.reason(feature);
        if (request.getRequestURI().endsWith(".html")) {
            response.sendRedirect("/featureDisabled.html?feature=" + feature);
            return false;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        String safeReason = reason == null ? "管理员暂未开放此功能" : reason.replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write("{\"code\":403,\"message\":\"该功能暂未开放\",\"reason\":\"" + safeReason + "\"}");
        return false;
    }

    private boolean requiresAssociatedLevel(String path) {
        return path.equals("/LLMindex.html") || path.equals("/projectDemo.html") || path.equals("/storageDemo.html");
    }

    private int requiredLevel(String path) {
        return path.equals("/projectDemo.html") || path.equals("/storageDemo.html") ? 2 : 1;
    }

    private boolean reject(HttpServletRequest request, HttpServletResponse response, String redirect)
            throws IOException {
        if (request.getRequestURI().endsWith(".html")) {
            response.sendRedirect(redirect);
            return false;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"code\":" + Result.CODE_UNAUTHORIZED + ",\"message\":\"请先登录\"}");
        return false;
    }

    private boolean rejectForbidden(HttpServletRequest request, HttpServletResponse response, String redirect)
            throws IOException {
        if (request.getRequestURI().endsWith(".html")) {
            response.sendRedirect(redirect);
            return false;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"code\":403,\"message\":\"当前等级无权访问\"}");
        return false;
    }
}
