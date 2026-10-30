package com.example.profilessystem.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface FeatureMapper {
    @Select("SELECT feature_key, enabled, reason FROM feature_toggle ORDER BY feature_key")
    List<Map<String,Object>> all();
    @Select("SELECT enabled FROM feature_toggle WHERE feature_key=#{key}")
    Integer enabled(String key);
    @Select("SELECT reason FROM feature_toggle WHERE feature_key=#{key}")
    String reason(String key);
    @Insert("INSERT INTO feature_toggle(feature_key,enabled,reason) VALUES(#{key},#{enabled},#{reason}) ON DUPLICATE KEY UPDATE enabled=#{enabled},reason=#{reason},updated_at=CURRENT_TIMESTAMP")
    int save(@Param("key") String key,@Param("enabled") int enabled,@Param("reason") String reason);
}
