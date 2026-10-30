package com.example.profilessystem.server;

import com.example.profilessystem.entity.User;
import com.example.profilessystem.mapper.UserMapper;
import com.example.profilessystem.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.List;

@Service
public class RecoveryService {
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final UserMapper userMapper;
    private final SecureRandom random = new SecureRandom();

    public RecoveryService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Transactional
    public Result<String> requestInvite(String username, String email, String reason) {
        User user = userMapper.findByName(username == null ? "" : username.trim());
        if (user == null || user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email == null ? "" : email.trim())) {
            return Result.fail("账户和注册邮箱不匹配");
        }
        return userMapper.createRecoveryRequest(user.getUsername(), "INVITE", reason) == 1
                ? Result.ok(null) : Result.fail("申请提交失败");
    }

    public List<User> pending() { return userMapper.findPendingRecovery(); }
    public List<User> all() { return userMapper.findAllRecovery(); }

    @Transactional
    public Result<String> approve(String username) {
        User user = userMapper.findByName(username);
        if (user == null || user.getRecoveryStatus() != 1) return Result.fail("申请不存在或已处理");
        String code = generateCode();
        if (userMapper.updateCode(user.getUsername(), code) != 1 || userMapper.updateRecoveryStatus(username, 2) != 1) return Result.fail("审批失败");
        return Result.ok(code);
    }

    @Transactional
    public Result<String> reject(String username) {
        return userMapper.updateRecoveryStatus(username, 3) == 1 ? Result.ok(null) : Result.fail("申请不存在或已处理");
    }

    private String generateCode() {
        for (int i = 0; i < 20; i++) {
            StringBuilder code = new StringBuilder();
            for (int j = 0; j < 10; j++) code.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            if (userMapper.findByCode(code.toString()) == null) return code.toString();
        }
        throw new IllegalStateException("身份码生成失败");
    }
}
