package com.example.profilessystem.mapper;

import com.example.profilessystem.entity.ChatMessage;
import com.example.profilessystem.entity.GroupMember;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SocialMapper {
    @Select("SELECT addressee AS username, status, 0 AS incoming FROM friendship WHERE requester=#{username} UNION ALL SELECT requester AS username, status, 1 AS incoming FROM friendship WHERE addressee=#{username}")
    List<java.util.Map<String,Object>> friends(String username);

    @Insert("INSERT INTO friendship(requester, addressee, status) VALUES(#{from},#{to},'PENDING')")
    int requestFriend(@Param("from") String from, @Param("to") String to);

    @Update("UPDATE friendship SET status=#{status} WHERE ((requester=#{from} AND addressee=#{to}) OR (requester=#{to} AND addressee=#{from})) AND status='PENDING'")
    int updateFriend(@Param("from") String from, @Param("to") String to, @Param("status") String status);

    @Select("SELECT status FROM friendship WHERE (requester=#{me} AND addressee=#{other}) OR (requester=#{other} AND addressee=#{me}) ORDER BY id DESC LIMIT 1")
    String friendStatus(@Param("me") String me, @Param("other") String other);

    @Select("SELECT COUNT(*) FROM friendship WHERE requester=#{other} AND addressee=#{me} AND status='PENDING'")
    int pendingIncoming(@Param("me") String me, @Param("other") String other);

    @Select("SELECT m.id, m.group_id, m.sender_username, m.receiver_username, m.content, m.created_at, COALESCE(u.level,0) sender_level FROM chat_message m LEFT JOIN user u ON u.username=m.sender_username WHERE m.group_id=#{groupId} ORDER BY m.created_at ASC LIMIT 200")
    List<ChatMessage> groupMessages(long groupId);

    @Select("SELECT m.id, m.group_id, m.sender_username, m.receiver_username, m.content, m.created_at, COALESCE(u.level,0) sender_level FROM chat_message m LEFT JOIN user u ON u.username=m.sender_username WHERE m.group_id=0 AND ((m.sender_username=#{me} AND m.receiver_username=#{other}) OR (m.sender_username=#{other} AND m.receiver_username=#{me})) ORDER BY m.created_at ASC LIMIT 200")
    List<ChatMessage> directMessages(@Param("me") String me, @Param("other") String other);

    @Insert("INSERT INTO chat_message(group_id,sender_username,receiver_username,content) VALUES(#{groupId},#{senderUsername},#{receiverUsername},#{content})")
    int insertMessage(ChatMessage message);

    @Select("SELECT m.username, m.role, COALESCE(u.level,0) level, COALESCE(m.member_level,0) member_level FROM study_group_member m LEFT JOIN user u ON u.username=m.username WHERE m.group_id=#{groupId} ORDER BY m.role='OWNER' DESC, m.joined_at ASC")
    List<GroupMember> members(long groupId);
}
