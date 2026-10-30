package com.example.profilessystem.mapper;

import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;

@Mapper
public interface LlmMapper {
    @Select("SELECT setting_value FROM llm_setting WHERE setting_key=#{key}")
    String getSetting(String key);
    @Insert("INSERT INTO llm_setting(setting_key,setting_value) VALUES(#{key},#{value}) ON DUPLICATE KEY UPDATE setting_value=#{value},updated_at=CURRENT_TIMESTAMP")
    int saveSetting(@Param("key") String key, @Param("value") String value);
    @Select("SELECT used_tokens FROM llm_usage WHERE username=#{username}")
    Integer usedTokens(String username);
    @Select("SELECT refreshed_at FROM llm_usage WHERE username=#{username}")
    LocalDateTime refreshedAt(String username);
    @Insert("INSERT INTO llm_usage(username,used_tokens,refreshed_at) VALUES(#{username},#{tokens},CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE used_tokens=used_tokens+#{tokens}")
    int addTokens(@Param("username") String username, @Param("tokens") int tokens);
    @Update("UPDATE llm_usage SET used_tokens=0,refreshed_at=CURRENT_TIMESTAMP")
    int resetAll();
    @Update("UPDATE llm_usage SET used_tokens=0,refreshed_at=CURRENT_TIMESTAMP WHERE username=#{username}")
    int resetUser(String username);
}
