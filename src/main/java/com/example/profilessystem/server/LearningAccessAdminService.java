package com.example.profilessystem.server;

import com.example.profilessystem.entity.LearningAccessTree;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LearningAccessAdminService {
    private final JdbcTemplate jdbc;

    public LearningAccessAdminService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public LearningAccessTree tree() {
        List<CategoryRow> categoryRows = jdbc.query(
                "SELECT id,slug,name,enabled FROM major_category ORDER BY sort_order,id",
                (rs, row) -> new CategoryRow(rs.getLong("id"), rs.getString("slug"),
                        rs.getString("name"), rs.getInt("enabled") == 1));

        Map<Long, List<LearningAccessTree.ContentNode>> majors = children(
                "SELECT category_id,id,name,code detail,enabled FROM major ORDER BY category_id,sort_order,id");
        Map<Long, List<LearningAccessTree.ContentNode>> routes = children(
                "SELECT category_id,id,title name,COALESCE(status,'') detail,enabled FROM learning_route " +
                        "ORDER BY category_id,sort_order,id");
        Map<Long, List<LearningAccessTree.ContentNode>> resources = children(
                "SELECT category_id,id,title name,CONCAT(resource_type,' · 等级',required_level) detail,enabled " +
                        "FROM learning_resource ORDER BY category_id,sort_order,id");

        List<LearningAccessTree.CategoryNode> categories = categoryRows.stream().map(category ->
                new LearningAccessTree.CategoryNode(category.id(), category.slug(), category.name(), category.enabled(),
                        majors.getOrDefault(category.id(), List.of()),
                        routes.getOrDefault(category.id(), List.of()),
                        resources.getOrDefault(category.id(), List.of()))).toList();
        return new LearningAccessTree(categories);
    }

    @Transactional
    public LearningAccessTree.UpdateResult setEnabled(String contentType, long id, boolean enabled) {
        String sql = switch (contentType) {
            case "category" -> "UPDATE major_category SET enabled=? WHERE id=?";
            case "major" -> "UPDATE major SET enabled=? WHERE id=?";
            case "route" -> "UPDATE learning_route SET enabled=? WHERE id=?";
            case "resource" -> "UPDATE learning_resource SET enabled=? WHERE id=?";
            default -> throw new IllegalArgumentException("不支持的学习内容类型");
        };
        if (jdbc.update(sql, enabled ? 1 : 0, id) != 1) {
            throw new IllegalArgumentException("找不到要设置的学习内容");
        }
        return new LearningAccessTree.UpdateResult(contentType, id, enabled);
    }

    private Map<Long, List<LearningAccessTree.ContentNode>> children(String sql) {
        Map<Long, List<LearningAccessTree.ContentNode>> result = new LinkedHashMap<>();
        jdbc.query(sql, rs -> {
            long categoryId = rs.getLong("category_id");
            result.computeIfAbsent(categoryId, ignored -> new ArrayList<>()).add(
                    new LearningAccessTree.ContentNode(rs.getLong("id"), rs.getString("name"),
                            rs.getString("detail"), rs.getInt("enabled") == 1));
        });
        return result;
    }

    private record CategoryRow(long id, String slug, String name, boolean enabled) {}
}
