package com.example.profilessystem.mapper;

import com.example.profilessystem.entity.CommunityRequest;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommunityRequestMapper {
    @Insert("INSERT INTO community_request(username, request_type, name, contact, category, message) "
            + "VALUES(#{username}, #{requestType}, #{name}, #{contact}, #{category}, #{message})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CommunityRequest request);

    @Select("SELECT id, request_type, name, contact, category, message, status, created_at, reviewed_at "
            + "FROM community_request WHERE status=0 ORDER BY created_at ASC")
    List<CommunityRequest> findPending();

    @Select("SELECT id, request_type, name, contact, category, message, status, created_at, reviewed_at "
            + "FROM community_request ORDER BY created_at DESC")
    List<CommunityRequest> findAll();

    @Update("UPDATE community_request SET status=#{status}, reviewed_at=CURRENT_TIMESTAMP WHERE id=#{id}")
    int updateStatus(@Param("id") long id, @Param("status") int status);

    @Select("SELECT id, username, request_type, name, contact, category, message, status, created_at, reviewed_at FROM community_request WHERE id=#{id}")
    CommunityRequest findById(long id);
}
