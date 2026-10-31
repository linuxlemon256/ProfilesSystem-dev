package com.example.profilessystem.server;

import com.example.profilessystem.entity.LearningContentTransfer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LearningContentTransferService {
    private static final int MAX_ITEMS_PER_SECTION = 10_000;
    private final JdbcTemplate jdbc;

    public LearningContentTransferService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Integer> overview() {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("professionalCategories", count("SELECT COUNT(*) FROM major_category"));
        result.put("specificMajors", count("SELECT COUNT(*) FROM major"));
        result.put("learningRoutes", count("SELECT COUNT(*) FROM learning_route"));
        result.put("learningResources", count("SELECT COUNT(*) FROM learning_resource"));
        return result;
    }

    public LearningContentTransfer exportContent() {
        List<LearningContentTransfer.CategoryItem> categories = jdbc.query(
                "SELECT slug,name,field_name,summary,icon,featured,enabled,sort_order FROM major_category ORDER BY sort_order,id",
                (rs, row) -> new LearningContentTransfer.CategoryItem(
                        rs.getString("slug"), rs.getString("name"), rs.getString("field_name"),
                        rs.getString("summary"), rs.getString("icon"), rs.getInt("featured") == 1,
                        rs.getInt("enabled") == 1, rs.getInt("sort_order")));

        List<LearningContentTransfer.MajorItem> majors = jdbc.query(
                "SELECT c.slug category_slug,m.code,m.name,m.program_type,m.summary,m.enabled,m.sort_order " +
                        "FROM major m JOIN major_category c ON c.id=m.category_id ORDER BY c.sort_order,m.sort_order,m.id",
                (rs, row) -> new LearningContentTransfer.MajorItem(
                        rs.getString("category_slug"), rs.getString("code"), rs.getString("name"),
                        rs.getString("program_type"), rs.getString("summary"), rs.getInt("enabled") == 1,
                        rs.getInt("sort_order")));

        List<LearningContentTransfer.RouteItem> routes = jdbc.query(
                "SELECT c.slug category_slug,m.code major_code,r.title,r.description,r.icon,r.target_url," +
                        "r.required_level,r.status,r.sort_order FROM learning_route r " +
                        "JOIN major_category c ON c.id=r.category_id LEFT JOIN major m ON m.id=r.major_id " +
                        "ORDER BY c.sort_order,r.sort_order,r.id",
                (rs, row) -> new LearningContentTransfer.RouteItem(
                        rs.getString("category_slug"), rs.getString("major_code"), rs.getString("title"),
                        rs.getString("description"), rs.getString("icon"), rs.getString("target_url"),
                        rs.getInt("required_level"), rs.getString("status"), rs.getInt("sort_order")));

        List<LearningContentTransfer.ResourceItem> resources = jdbc.query(
                "SELECT c.slug category_slug,m.code major_code,r.title route_title,x.title,x.description,x.icon," +
                        "x.resource_type,x.required_level,x.status,x.target_url,x.original_name,x.storage_key,x.sort_order " +
                        "FROM learning_resource x JOIN major_category c ON c.id=x.category_id " +
                        "LEFT JOIN major m ON m.id=x.major_id LEFT JOIN learning_route r ON r.id=x.route_id " +
                        "ORDER BY c.sort_order,x.sort_order,x.id",
                (rs, row) -> new LearningContentTransfer.ResourceItem(
                        rs.getString("category_slug"), rs.getString("major_code"), rs.getString("route_title"),
                        rs.getString("title"), rs.getString("description"), rs.getString("icon"),
                        rs.getString("resource_type"), rs.getInt("required_level"), rs.getString("status"),
                        rs.getString("target_url"), rs.getString("original_name"), rs.getString("storage_key"),
                        rs.getInt("sort_order")));

        return new LearningContentTransfer(LearningContentTransfer.FORMAT, LearningContentTransfer.VERSION,
                Instant.now(), categories, majors, routes, resources);
    }

    @Transactional
    public LearningContentTransfer.ImportSummary importContent(LearningContentTransfer content) {
        validatePackage(content);
        List<LearningContentTransfer.CategoryItem> categories = safe(content.professionalCategories());
        List<LearningContentTransfer.MajorItem> majors = safe(content.specificMajors());
        List<LearningContentTransfer.RouteItem> routes = safe(content.learningRoutes());
        List<LearningContentTransfer.ResourceItem> resources = safe(content.learningResources());

        categories.forEach(this::upsertCategory);
        majors.forEach(this::upsertMajor);
        routes.forEach(this::upsertRoute);
        resources.forEach(this::upsertResource);

        return new LearningContentTransfer.ImportSummary(categories.size(), majors.size(), routes.size(), resources.size());
    }

    private void upsertCategory(LearningContentTransfer.CategoryItem item) {
        required(item.slug(), "专业类别标识");
        required(item.name(), "专业类别名称");
        jdbc.update("INSERT INTO major_category(slug,name,field_name,summary,icon,featured,enabled,sort_order) " +
                        "VALUES(?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE name=VALUES(name),field_name=VALUES(field_name)," +
                        "summary=VALUES(summary),icon=VALUES(icon),featured=VALUES(featured),enabled=VALUES(enabled),sort_order=VALUES(sort_order)",
                item.slug().trim(), item.name().trim(), text(item.fieldName()), text(item.summary()),
                defaultText(item.icon(), "✦"), bool(item.featured(), false), bool(item.enabled(), true), number(item.sortOrder(), 0));
    }

    private void upsertMajor(LearningContentTransfer.MajorItem item) {
        required(item.categorySlug(), "具体专业所属类别");
        required(item.code(), "具体专业代码");
        required(item.name(), "具体专业名称");
        long categoryId = categoryId(item.categorySlug());
        jdbc.update("INSERT INTO major(category_id,code,name,program_type,summary,enabled,sort_order) VALUES(?,?,?,?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE category_id=VALUES(category_id),name=VALUES(name),program_type=VALUES(program_type)," +
                        "summary=VALUES(summary),enabled=VALUES(enabled),sort_order=VALUES(sort_order)",
                categoryId, item.code().trim(), item.name().trim(), defaultText(item.programType(), "BASIC").toUpperCase(),
                text(item.summary()), bool(item.enabled(), true), number(item.sortOrder(), 0));
    }

    private void upsertRoute(LearningContentTransfer.RouteItem item) {
        required(item.categorySlug(), "学习路线所属类别");
        required(item.title(), "学习路线标题");
        long categoryId = categoryId(item.categorySlug());
        Long majorId = optionalMajorId(item.majorCode());
        jdbc.update("INSERT INTO learning_route(category_id,major_id,title,description,icon,target_url,required_level,status,sort_order) " +
                        "VALUES(?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE major_id=VALUES(major_id),description=VALUES(description)," +
                        "icon=VALUES(icon),target_url=VALUES(target_url),required_level=VALUES(required_level),status=VALUES(status),sort_order=VALUES(sort_order)",
                categoryId, majorId, item.title().trim(), text(item.description()), defaultText(item.icon(), "⌘"),
                nullable(item.targetUrl()), level(item.requiredLevel()), defaultText(item.status(), "DRAFT").toUpperCase(),
                number(item.sortOrder(), 0));
    }

    private void upsertResource(LearningContentTransfer.ResourceItem item) {
        required(item.categorySlug(), "学习资源所属类别");
        required(item.title(), "学习资源标题");
        long categoryId = categoryId(item.categorySlug());
        Long majorId = optionalMajorId(item.majorCode());
        Long routeId = optionalRouteId(categoryId, item.routeTitle());
        jdbc.update("INSERT INTO learning_resource(category_id,major_id,route_id,title,description,icon,resource_type," +
                        "required_level,status,target_url,original_name,storage_key,sort_order) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE major_id=VALUES(major_id),route_id=VALUES(route_id),description=VALUES(description)," +
                        "icon=VALUES(icon),resource_type=VALUES(resource_type),required_level=VALUES(required_level),status=VALUES(status)," +
                        "target_url=VALUES(target_url),original_name=VALUES(original_name),storage_key=VALUES(storage_key),sort_order=VALUES(sort_order)",
                categoryId, majorId, routeId, item.title().trim(), text(item.description()), defaultText(item.icon(), "✦"),
                defaultText(item.resourceType(), "LINK").toUpperCase(), level(item.requiredLevel()),
                defaultText(item.status(), "DRAFT").toUpperCase(), nullable(item.targetUrl()), nullable(item.originalName()),
                nullable(item.storageKey()), number(item.sortOrder(), 0));
    }

    private void validatePackage(LearningContentTransfer content) {
        if (content == null || !LearningContentTransfer.FORMAT.equals(content.format()) ||
                content.formatVersion() == null || content.formatVersion() != LearningContentTransfer.VERSION) {
            throw new IllegalArgumentException("文件不是受支持的 ProfilesSystem 学习内容包");
        }
        checkSize(content.professionalCategories(), "专业类别");
        checkSize(content.specificMajors(), "具体专业");
        checkSize(content.learningRoutes(), "学习路线");
        checkSize(content.learningResources(), "学习资源");
    }

    private void checkSize(List<?> list, String name) {
        if (list != null && list.size() > MAX_ITEMS_PER_SECTION) {
            throw new IllegalArgumentException(name + "一次最多导入 " + MAX_ITEMS_PER_SECTION + " 条");
        }
    }

    private int count(String sql) {
        Integer value = jdbc.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private long categoryId(String slug) {
        List<Long> ids = jdbc.query("SELECT id FROM major_category WHERE slug=?", (rs, row) -> rs.getLong(1), slug.trim());
        if (ids.isEmpty()) throw new IllegalArgumentException("找不到专业类别：" + slug);
        return ids.get(0);
    }

    private Long optionalMajorId(String code) {
        if (code == null || code.isBlank()) return null;
        List<Long> ids = jdbc.query("SELECT id FROM major WHERE code=?", (rs, row) -> rs.getLong(1), code.trim());
        if (ids.isEmpty()) throw new IllegalArgumentException("找不到具体专业：" + code);
        return ids.get(0);
    }

    private Long optionalRouteId(long categoryId, String title) {
        if (title == null || title.isBlank()) return null;
        List<Long> ids = jdbc.query("SELECT id FROM learning_route WHERE category_id=? AND title=?",
                (rs, row) -> rs.getLong(1), categoryId, title.trim());
        if (ids.isEmpty()) throw new IllegalArgumentException("找不到学习路线：" + title);
        return ids.get(0);
    }

    private void required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + "不能为空");
    }

    private String text(String value) { return value == null ? "" : value.trim(); }
    private String nullable(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String defaultText(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
    private int bool(Boolean value, boolean fallback) { return Boolean.TRUE.equals(value) || value == null && fallback ? 1 : 0; }
    private int number(Integer value, int fallback) { return value == null ? fallback : value; }
    private int level(Integer value) { return Math.max(0, Math.min(2, number(value, 0))); }
    private <T> List<T> safe(List<T> value) { return value == null ? List.of() : value; }
}
