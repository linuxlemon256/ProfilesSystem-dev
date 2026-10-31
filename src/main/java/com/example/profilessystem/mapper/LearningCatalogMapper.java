package com.example.profilessystem.mapper;

import com.example.profilessystem.entity.LearningRoute;
import com.example.profilessystem.entity.Major;
import com.example.profilessystem.entity.MajorCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LearningCatalogMapper {
    @Select("SELECT id,slug,name,field_name,summary,icon,featured,enabled,sort_order FROM major_category WHERE enabled=1 ORDER BY sort_order,id")
    List<MajorCategory> findCategories();

    @Select("SELECT id,slug,name,field_name,summary,icon,featured,enabled,sort_order FROM major_category WHERE slug=#{slug} AND enabled=1")
    MajorCategory findCategoryBySlug(String slug);

    @Select("SELECT id,category_id,code,name,program_type,summary,enabled,sort_order FROM major WHERE category_id=#{categoryId} AND enabled=1 ORDER BY sort_order,id")
    List<Major> findMajors(Long categoryId);

    @Select("SELECT id,category_id,major_id,title,description,icon,target_url,required_level,status,sort_order FROM learning_route WHERE category_id=#{categoryId} AND status='PUBLISHED' AND required_level<=#{level} ORDER BY sort_order,id")
    List<LearningRoute> findRoutes(@Param("categoryId") Long categoryId, @Param("level") int level);
}
