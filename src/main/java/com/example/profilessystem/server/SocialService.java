package com.example.profilessystem.server;

import com.example.profilessystem.entity.ChatMessage;
import com.example.profilessystem.mapper.GroupMapper;
import com.example.profilessystem.mapper.SocialMapper;
import com.example.profilessystem.mapper.UserMapper;
import com.example.profilessystem.result.Result;
import org.springframework.stereotype.Service;
import java.util.List;
import com.example.profilessystem.entity.GroupMember;

@Service
public class SocialService {
    private final SocialMapper mapper;
    private final GroupMapper groupMapper;
    private final UserMapper userMapper;
    public SocialService(SocialMapper mapper, GroupMapper groupMapper, UserMapper userMapper) { this.mapper=mapper; this.groupMapper=groupMapper; this.userMapper=userMapper; }
    public Result<List<java.util.Map<String,Object>>> friends(String username) { return Result.ok(mapper.friends(username)); }
    public Result<String> requestFriend(String from, String to) {
        if (to == null || to.isBlank() || from.equals(to) || userMapper.findByName(to) == null) return Result.fail(Result.CODE_BAD_REQUEST,"找不到这个账户");
        try { return mapper.requestFriend(from,to)==1 ? Result.ok(null) : Result.fail("好友申请已存在"); } catch (Exception e) { return Result.fail("好友申请已存在"); }
    }
    public Result<String> updateFriend(String me, String other, String status) { if (!"ACCEPTED".equals(status) && !"REJECTED".equals(status)) return Result.fail(Result.CODE_BAD_REQUEST,"无效的好友操作"); if (mapper.pendingIncoming(me, other) == 0) return Result.fail("没有待处理的好友申请"); return mapper.updateFriend(me,other,status)==1?Result.ok(null):Result.fail("好友申请不存在"); }
    public Result<List<ChatMessage>> groupMessages(long groupId, String user) { com.example.profilessystem.entity.Group g=groupMapper.findById(groupId); if(g==null||g.getApprovalStatus()!=1||groupMapper.isMember(groupId,user)==0) return Result.fail(Result.CODE_FORBIDDEN,"学习组尚未通过审核或你不是成员"); return Result.ok(mapper.groupMessages(groupId)); }
    public Result<List<ChatMessage>> directMessages(String me, String other) { if (other==null || userMapper.findByName(other)==null) return Result.fail("账户不存在"); if (!"ACCEPTED".equals(mapper.friendStatus(me, other))) return Result.fail(Result.CODE_FORBIDDEN,"请先通过好友申请"); return Result.ok(mapper.directMessages(me,other)); }
    public Result<List<GroupMember>> groupMembers(long groupId, String user) { if(groupMapper.isMember(groupId,user)==0)return Result.fail(Result.CODE_FORBIDDEN,"请先加入学习组"); return Result.ok(mapper.members(groupId)); }
    public Result<String> sendGroup(long groupId, String user, String content) { com.example.profilessystem.entity.Group g=groupMapper.findById(groupId); if(g==null||g.getApprovalStatus()!=1||groupMapper.isMember(groupId,user)==0) return Result.fail(Result.CODE_FORBIDDEN,"学习组尚未通过审核或你不是成员"); return send(groupId,user,null,content); }
    public Result<String> sendDirect(String me, String other, String content) { if (!"ACCEPTED".equals(mapper.friendStatus(me, other))) return Result.fail(Result.CODE_FORBIDDEN,"请先通过好友申请"); return send(0,me,other,content); }
    private Result<String> send(long groupId,String sender,String receiver,String content) { if(content==null||content.isBlank()||content.length()>2000)return Result.fail(Result.CODE_BAD_REQUEST,"消息不能为空且不能超过 2000 字"); ChatMessage m=new ChatMessage();m.setGroupId(groupId);m.setSenderUsername(sender);m.setReceiverUsername(receiver);m.setContent(content.trim());return mapper.insertMessage(m)==1?Result.ok(null):Result.fail("发送失败"); }
}
