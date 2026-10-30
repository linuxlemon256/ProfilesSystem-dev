package com.example.profilessystem.server;

import com.example.profilessystem.entity.CommunityRequest;
import com.example.profilessystem.entity.CommunityMessage;
import com.example.profilessystem.mapper.CommunityRequestMapper;
import com.example.profilessystem.mapper.CommunityMessageMapper;
import com.example.profilessystem.mapper.UserMapper;
import com.example.profilessystem.mapper.NotificationMapper;
import com.example.profilessystem.entity.Notification;
import com.example.profilessystem.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class CommunityRequestService {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+[.][A-Za-z]{2,}$");
    private final CommunityRequestMapper mapper;
    private final CommunityMessageMapper messageMapper;
    private final UserMapper userMapper;
    private final NotificationMapper notificationMapper;

    public CommunityRequestService(CommunityRequestMapper mapper, CommunityMessageMapper messageMapper, UserMapper userMapper, NotificationMapper notificationMapper) {
        this.mapper = mapper;
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.notificationMapper = notificationMapper;
    }

    @Transactional
    public Result<String> submit(CommunityRequest request) {
        if (request == null || request.getName() == null || request.getName().isBlank()
                || request.getMessage() == null || request.getMessage().isBlank()
                || request.getContact() == null || request.getContact().isBlank()) {
            return Result.fail(Result.CODE_BAD_REQUEST, "请填写称呼、邮箱和具体内容");
        }
        if (!"feedback".equals(request.getRequestType()) && !"join".equals(request.getRequestType())) {
            return Result.fail(Result.CODE_BAD_REQUEST, "申请类型不正确");
        }
        request.setName(request.getName().trim());
        request.setContact(request.getContact().trim());
        if (!EMAIL_PATTERN.matcher(request.getContact()).matches()) {
            return Result.fail(Result.CODE_BAD_REQUEST, "请输入有效的邮箱地址");
        }
        request.setMessage(request.getMessage().trim());
        if (mapper.insert(request) != 1) return Result.fail("提交失败");
        CommunityMessage message = new CommunityMessage();
        message.setRequestId(request.getId());
        message.setSenderType("USER");
        message.setSenderName(request.getName());
        message.setContent(request.getMessage());
        messageMapper.insert(message);
        return Result.ok(null);
    }

    @Transactional(readOnly = true)
    public Result<List<CommunityRequest>> pending() { return Result.ok(mapper.findPending()); }

    @Transactional(readOnly = true)
    public Result<List<CommunityRequest>> all() { return Result.ok(mapper.findAll()); }

    @Transactional
    public Result<String> updateStatus(long id, int status) {
        if (status < 1 || status > 2) return Result.fail(Result.CODE_BAD_REQUEST, "处理状态不正确");
        return mapper.updateStatus(id, status) == 1 ? Result.ok(null) : Result.fail("申请不存在或已处理");
    }

    @Transactional(readOnly = true)
    public Result<List<CommunityMessage>> messagesForUser(String username) {
        var user = userMapper.findByName(username);
        String email = user == null || user.getEmail() == null ? "" : user.getEmail();
        return Result.ok(messageMapper.findForUser(username, email));
    }

    @Transactional(readOnly = true)
    public Result<List<CommunityMessage>> messagesForRequest(long requestId) {
        return Result.ok(messageMapper.findByRequestId(requestId));
    }

    @Transactional
    public Result<String> replyAsAdmin(long requestId, String content) {
        if (content == null || content.isBlank()) return Result.fail(Result.CODE_BAD_REQUEST, "回复内容不能为空");
        CommunityRequest request = mapper.findById(requestId);
        if (request == null) return Result.fail(Result.CODE_BAD_REQUEST, "申请不存在");
        CommunityMessage message = new CommunityMessage();
        message.setRequestId(requestId);
        message.setSenderType("ADMIN");
        message.setSenderName("管理员");
        message.setContent(content.trim());
        if (messageMapper.insert(message) != 1) return Result.fail("回复失败");
        String username = request.getUsername();
        if ((username == null || username.isBlank()) && request.getContact() != null) {
            var user = userMapper.findByEmail(request.getContact());
            username = user == null ? null : user.getUsername();
        }
        if (username != null && !username.isBlank()) {
            Notification notification = new Notification();
            notification.setUsername(username);
            notification.setSenderName("管理员");
            notification.setContent("关于你的" + ("join".equals(request.getRequestType()) ? "加入申请" : "问题反馈") + "：" + content.trim());
            notificationMapper.insert(notification);
        }
        return Result.ok(null);
    }

    @Transactional
    public Result<String> replyAsUser(long requestId, String username, String content) {
        if (content == null || content.isBlank()) return Result.fail(Result.CODE_BAD_REQUEST, "消息内容不能为空");
        var user = userMapper.findByName(username);
        if (user == null) return Result.fail(Result.CODE_UNAUTHORIZED, "账户不存在");
        var messages = messageMapper.findForUser(username, user.getEmail() == null ? "" : user.getEmail());
        boolean ownsRequest = messages.stream().anyMatch(m -> requestId == m.getRequestId());
        if (!ownsRequest) return Result.fail(Result.CODE_UNAUTHORIZED, "无权访问该交流内容");
        CommunityMessage message = new CommunityMessage();
        message.setRequestId(requestId);
        message.setSenderType("USER");
        message.setSenderName(username);
        message.setContent(content.trim());
        return messageMapper.insert(message) == 1 ? Result.ok(null) : Result.fail("发送失败");
    }
}
