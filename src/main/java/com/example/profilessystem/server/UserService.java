package com.example.profilessystem.server;

import com.example.profilessystem.entity.LoginBody;
import com.example.profilessystem.entity.RegisterBody;
import com.example.profilessystem.entity.User;
import com.example.profilessystem.entity.UserAdminUpdateBody;
import com.example.profilessystem.mapper.UserMapper;
import com.example.profilessystem.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserService {

    /** 身份码字符集：去掉 0/O、1/I 等易混字符。 */
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** 常见邮箱格式：本地部分@域名.顶级域，不追求 RFC 全覆盖。 */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+[.][A-Za-z]{2,}$");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }
    @Transactional
    public Result<String> create(User user) {
        Result<User> existing = findByName(user.getUsername());
        if (existing.getCode() == Result.CODE_SUCCESS) {
            return Result.fail("用户名已存在");
        }
        int row = userMapper.insert(user);
        if (row != 1) {
            return Result.fail("插入失败");
        }
        return Result.ok(null);
    }
    @Transactional
    public Result<String> register(RegisterBody registerBody) {
        if (userMapper.findByName(registerBody.getUsername()) != null) {
            return Result.fail("用户名已存在");
        }
        // 申请原因对应 varchar(255)，超长会让插入直接失败，这里提前拦下
        String reason = registerBody.getReason();
        if (reason != null && reason.length() > 255) {
            return Result.fail("申请原因不能超过 255 字");
        }
        // 邮箱必填：先校验非空，再校验格式
        String email = registerBody.getEmail() == null ? "" : registerBody.getEmail().trim();
        if (email.isEmpty()) {
            return Result.fail("请填写邮箱");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return Result.fail("邮箱格式不正确");
        }
        if (userMapper.findByEmail(email) != null) {
            return Result.fail("邮箱已被注册");
        }
        String password = registerBody.getPassword() == null ? "" : registerBody.getPassword();
        if (password.length() < 6) {
            return Result.fail("密码至少需要 6 位");
        }
        User user = new User(registerBody.getUsername(), passwordEncoder.encode(password),"",null,0,email,registerBody.getReason());
        int row = userMapper.insert(user);
        if (row != 1) {
            return Result.fail("插入失败");
        }
        return Result.ok(null);
    }
    @Transactional
    public Result<String> deleteByName(String name) {
        int row = userMapper.deleteByName(name);
        if (row != 1) {
            return Result.fail("删除失败");
        }
        return Result.ok(null);
    }
    @Transactional
    public Result<User> findByName(String name) {
        User user = userMapper.findByName(name);
        if (user == null) {
            return Result.fail("未找到该用户");
        }
        return Result.ok(user);
    }
    /** 审核通过：isin 置 1；没有身份码时生成一个并返回。 */
    @Transactional
    public Result<String> update(String name) {
        User user = userMapper.findByName(name);
        if (user == null) {
            return Result.fail("未找到该用户");
        }
        int row = userMapper.updateIsin(name, 1);
        if (row != 1) {
            return Result.fail("添加失败");
        }
        String code = user.getCode();
        if (code == null || code.isBlank()) {
            code = generateInviteCode();
            if (code == null) {
                return Result.fail("身份码生成失败");
            }
            if (userMapper.updateCode(name, code) != 1) {
                return Result.fail("身份码保存失败");
            }
        }
        return Result.ok(code);
    }

    /** 身份码登录：账户 + 身份码都匹配，且已审核通过。 */
    @Transactional
    public Result<String> login(LoginBody loginBody) {
        String username = loginBody.getUsername() == null ? "" : loginBody.getUsername().trim();
        String code = loginBody.getCode() == null ? "" : loginBody.getCode().trim().toUpperCase();
        if (username.isEmpty() || code.isEmpty()) {
            return Result.fail("请填写账户和身份码");
        }
        User user = userMapper.findByName(username);
        if (user == null || user.getCode() == null || !user.getCode().equalsIgnoreCase(code)) {
            return Result.fail("账户或身份码错误");
        }
        if (user.getIsin() != 1) {
            return Result.fail("账户尚未通过审核");
        }
        return Result.ok(user.getUsername());
    }

    /** 账户 + 密码登录。 */
    @Transactional
    public Result<String> loginByPassword(LoginBody loginBody) {
        String username = loginBody.getUsername() == null ? "" : loginBody.getUsername().trim();
        String password = loginBody.getPassword() == null ? "" : loginBody.getPassword();
        if (username.isEmpty() || password.isEmpty()) return Result.fail("请填写账户和密码");
        User user = userMapper.findByName(username);
        if (user == null || user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            return Result.fail("账户或密码错误");
        }
        if (user.getIsin() != 1) return Result.fail("账户尚未通过审核");
        return Result.ok(user.getUsername());
    }

    /** 邮箱 + 密码登录。 */
    @Transactional
    public Result<String> loginByEmail(LoginBody loginBody) {
        String email = loginBody.getEmail() == null ? "" : loginBody.getEmail().trim();
        String password = loginBody.getPassword() == null ? "" : loginBody.getPassword();
        if (email.isEmpty() || password.isEmpty()) return Result.fail("请填写邮箱和密码");
        User user = userMapper.findByEmail(email);
        if (user == null || user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            return Result.fail("邮箱或密码错误");
        }
        if (user.getIsin() != 1) return Result.fail("账户尚未通过审核");
        return Result.ok(user.getUsername());
    }

    @Transactional
    public Result<String> resetPassword(String username, String code, String password) {
        User user = userMapper.findByName(username);
        if (user == null || user.getCode() == null || !user.getCode().equalsIgnoreCase(code)) {
            return Result.fail("账户或身份码错误");
        }
        if (password == null || password.length() < 6) return Result.fail("密码至少需要 6 位");
        return userMapper.updatePassword(username, passwordEncoder.encode(password)) == 1
                ? Result.ok(null) : Result.fail("密码更新失败");
    }

    @Transactional
    public Result<String> findAccountByEmail(String email) {
        User user = userMapper.findByEmail(email == null ? "" : email.trim());
        return user == null ? Result.fail("没有找到对应账户") : Result.ok(user.getUsername());
    }

    @Transactional
    public Result<String> updateByAdmin(String username, UserAdminUpdateBody update) {
        User user = userMapper.findByName(username);
        if (user == null) return Result.fail("未找到该用户");
        if (update.getLevel() != null && (update.getLevel() < 0 || update.getLevel() > 2)) {
            return Result.fail("用户等级必须是 0、1 或 2");
        }
        if (update.getEmail() != null) {
            String email = update.getEmail().trim();
            if (!email.isEmpty() && !email.equalsIgnoreCase(user.getEmail())) {
                User sameEmail = userMapper.findByEmail(email);
                if (sameEmail != null && !sameEmail.getUsername().equals(username)) return Result.fail("邮箱已被其他账户使用");
            }
            update.setEmail(email);
        }
        if (update.getPassword() != null && !update.getPassword().isBlank()) {
            if (update.getPassword().length() < 6) return Result.fail("密码至少需要 6 位");
            update.setPassword(passwordEncoder.encode(update.getPassword()));
        } else {
            update.setPassword(null);
        }
        return userMapper.updateByAdmin(username, update) == 1 ? Result.ok(null) : Result.fail("用户信息未发生变化");
    }

    /** 生成不重复的随机身份码；多次冲突时返回 null。 */
    private String generateInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (userMapper.findByCode(code) == null) {
                return code;
            }
        }
        return null;
    }
    /** 审核拒绝：isin 置 2。 */
    @Transactional
    public Result<String> reject(String name) {
        User user = userMapper.findByName(name);
        if (user == null) {
            return Result.fail("未找到该用户");
        }
        int row = userMapper.updateIsin(name, 2);
        if (row != 1) {
            return Result.fail("添加失败");
        }
        return Result.ok(null);
    }
    @Transactional
    public Result<List<User>> findAllUncommit() {
        List<User> users = userMapper.findByIsIn(0);
        return Result.ok(users);
    }
    @Transactional
    public Result<List<User>> findAll() {
        List<User> users = userMapper.findAll();
        return Result.ok(users);
    }
    @Transactional
    public Result<List<User>> findAllCommit() {
        List<User> users = userMapper.findByIsIn(1);
        return Result.ok(users);
    }
    @Transactional
    public Result<List<User>> findAllRejected() {
        List<User> users = userMapper.findByIsIn(2);
        return Result.ok(users);
    }

}
