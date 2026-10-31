package com.example.profilessystem.mapper;

import com.example.profilessystem.entity.LearningResource;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LearningResourceMapper {
    String SELECT_COLUMNS = "SELECT r.id,r.category_id,r.major_id,r.route_id,c.slug category_slug,c.name category_name,m.code major_code,m.name major_name,r.title,r.description,r.icon,r.resource_type,r.required_level,r.status,r.enabled,r.target_url,r.original_name,r.storage_key,r.sort_order,r.created_at,r.updated_at FROM learning_resource r JOIN major_category c ON c.id=r.category_id LEFT JOIN major m ON m.id=r.major_id ";

    @Select({"<script>", SELECT_COLUMNS,
            "WHERE r.status='PUBLISHED' AND r.enabled=1 AND c.enabled=1 AND r.required_level&lt;=#{level}",
            "AND (r.major_id IS NULL OR m.enabled=1)",
            "AND (r.route_id IS NULL OR EXISTS (SELECT 1 FROM learning_route lr WHERE lr.id=r.route_id AND lr.enabled=1))",
            "<if test='categorySlug != null and categorySlug != \"\"'>AND c.slug=#{categorySlug}</if>",
            "<if test='majorCode != null and majorCode != \"\"'>AND m.code=#{majorCode}</if>",
            "<if test='query != null and query != \"\"'>AND (r.title LIKE CONCAT('%',#{query},'%') OR r.description LIKE CONCAT('%',#{query},'%'))</if>",
            "ORDER BY c.sort_order,r.sort_order,r.id", "</script>"})
    List<LearningResource> findVisible(@Param("categorySlug") String categorySlug,
                                       @Param("majorCode") String majorCode,
                                       @Param("query") String query,
                                       @Param("level") int level);

    @Select(SELECT_COLUMNS + "WHERE r.id=#{id} AND r.status='PUBLISHED' AND r.enabled=1 AND c.enabled=1 " +
            "AND r.required_level<=#{level} AND (r.major_id IS NULL OR m.enabled=1) " +
            "AND (r.route_id IS NULL OR EXISTS (SELECT 1 FROM learning_route lr WHERE lr.id=r.route_id AND lr.enabled=1))")
    LearningResource findVisibleById(@Param("id") long id, @Param("level") int level);

    @Select(SELECT_COLUMNS + "ORDER BY r.updated_at DESC,r.id DESC")
    List<LearningResource> findAllForAdmin();

    @Insert("INSERT INTO learning_resource(category_id,major_id,route_id,title,description,icon,resource_type,required_level,status,enabled,target_url,original_name,storage_key,sort_order) VALUES(#{categoryId},#{majorId},#{routeId},#{title},#{description},#{icon},#{resourceType},#{requiredLevel},#{status},#{enabled},#{targetUrl},#{originalName},#{storageKey},#{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LearningResource resource);

    @Update("UPDATE learning_resource SET category_id=#{categoryId},major_id=#{majorId},route_id=#{routeId},title=#{title},description=#{description},icon=#{icon},resource_type=#{resourceType},required_level=#{requiredLevel},status=#{status},enabled=#{enabled},target_url=#{targetUrl},original_name=#{originalName},storage_key=#{storageKey},sort_order=#{sortOrder},updated_at=CURRENT_TIMESTAMP WHERE id=#{id}")
    int update(LearningResource resource);

    @Delete("DELETE FROM learning_resource WHERE id=#{id}")
    int delete(long id);
}
