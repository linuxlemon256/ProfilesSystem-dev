package com.example.profilessystem.controller;

import com.example.profilessystem.auth.SessionKeys;
import com.example.profilessystem.entity.LlmChatBody;
import com.example.profilessystem.result.Result;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import com.example.profilessystem.server.LlmClientService;
import com.example.profilessystem.server.LlmQuotaService;

@RestController
@RequestMapping("/api/llm")
public class LlmController {
    private final LlmQuotaService quotaService;
    private final LlmClientService llmClientService;
    public LlmController(LlmQuotaService quotaService, LlmClientService llmClientService) {
        this.quotaService = quotaService;
        this.llmClientService = llmClientService;
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> status(HttpSession session) {
        if (session.getAttribute(SessionKeys.USER) == null) return Result.fail(Result.CODE_UNAUTHORIZED, "请先登录");
        boolean configured = quotaService.modelConfigured();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("configured", configured);
        data.put("mode", configured ? "connected" : "demo");
        data.put("provider", quotaService.provider());
        data.put("modelName", quotaService.modelName());
        data.put("baseUrl", quotaService.baseUrl());
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

        try {
            LlmClientService.ChatResult response = llmClientService.chat(message);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("reply", response.reply());
            data.put("mode", "connected");
            data.put("provider", quotaService.provider());
            data.put("modelName", quotaService.modelName());
            data.put("totalTokens", response.totalTokens());
            data.put("username", username.toString());
            return Result.ok(data);
        } catch (IllegalStateException exception) {
            return Result.fail(Result.CODE_ERROR, exception.getMessage());
        }
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
