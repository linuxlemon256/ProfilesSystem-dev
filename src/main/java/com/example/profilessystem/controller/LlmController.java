package com.example.profilessystem.controller;

import com.example.profilessystem.auth.SessionKeys;
import com.example.profilessystem.entity.LlmChatBody;
import com.example.profilessystem.result.Result;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.LinkedHashMap;
import java.util.Map;
import com.example.profilessystem.server.LlmQuotaService;

@RestController
@RequestMapping("/api/llm")
public class LlmController {
    private final LlmQuotaService quotaService;
    public LlmController(LlmQuotaService quotaService) { this.quotaService = quotaService; }
    @Value("${LLM_API_KEY:}")
    private String apiKey;
    @Value("${LLM_BASE_URL:}")
    private String baseUrl;

    @GetMapping("/status")
    public Result<Map<String, Object>> status(HttpSession session) {
        if (session.getAttribute(SessionKeys.USER) == null) return Result.fail(Result.CODE_UNAUTHORIZED, "请先登录");
        boolean configured = quotaService.modelConfigured();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("configured", configured);
        data.put("mode", configured ? "connected" : "demo");
        data.put("provider", configured ? "external" : "placeholder");
        data.put("modelName", quotaService.adminSettings().getData().get("modelName"));
        data.put("baseUrl", quotaService.adminSettings().getData().get("baseUrl"));
        return Result.ok(data);
    }

    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody LlmChatBody body, HttpSession session) {
        Object username = session.getAttribute(SessionKeys.USER);
        if (username == null) return Result.fail(Result.CODE_UNAUTHORIZED, "请先登录");
        if (body == null || body.getMessage() == null || body.getMessage().isBlank()) {
            return Result.fail(Result.CODE_BAD_REQUEST, "请输入问题");
        }
        String message = body.getMessage().trim();
        if (message.length() > 4000) return Result.fail(Result.CODE_BAD_REQUEST, "问题不能超过 4000 字");
        int estimatedTokens = Math.max(1, (int) Math.ceil(message.length() / 2.5));
        Result<Map<String,Object>> quota = quotaService.consume(username.toString(), estimatedTokens);
        if (quota.getCode() != Result.CODE_SUCCESS) return quota;

        // 当前为后端联调回复；以后只需要在这里接入本地或云端模型服务。
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reply", "已收到你的问题：\n\n" + message + "\n\n这是 LLM 学习助手的后端联调回复。模型服务接入后，我会在这里给出真正的知识讲解、代码分析和学习建议。\n\n你可以继续补充：目标、当前代码、报错信息或希望达到的结果。" );
        data.put("mode", "demo");
        data.put("username", username.toString());
        return Result.ok(data);
    }

    @GetMapping("/quota")
    public Result<Map<String,Object>> quota(HttpSession session) {
        Object username = session.getAttribute(SessionKeys.USER);
        return username == null ? Result.fail(Result.CODE_UNAUTHORIZED, "请先登录") : quotaService.status(username.toString());
    }

    @GetMapping("/admin/settings")
    public Result<Map<String,Object>> adminSettings() { return quotaService.adminSettings(); }

    @PostMapping("/admin/settings")
    public Result<String> saveSettings(@RequestBody com.example.profilessystem.entity.LlmAdminSettingsBody body) { return quotaService.saveAdminSettings(body); }

    @PostMapping("/admin/refresh")
    public Result<String> refresh() { return quotaService.resetAll(); }
}
