package com.example.profilessystem.entity;

import java.time.Instant;
import java.util.List;

public record LearningContentTransfer(
        String format,
        Integer formatVersion,
        Instant exportedAt,
        List<CategoryItem> professionalCategories,
        List<MajorItem> specificMajors,
        List<RouteItem> learningRoutes,
        List<ResourceItem> learningResources
) {
    public static final String FORMAT = "profiles-learning-content";
    public static final int VERSION = 1;

    public record CategoryItem(
            String slug,
            String name,
            String fieldName,
            String summary,
            String icon,
            Boolean featured,
            Boolean enabled,
            Integer sortOrder
    ) {}

    public record MajorItem(
            String categorySlug,
            String code,
            String name,
            String programType,
            String summary,
            Boolean enabled,
            Integer sortOrder
    ) {}

    public record RouteItem(
            String categorySlug,
            String majorCode,
            String title,
            String description,
            String icon,
            String targetUrl,
            Integer requiredLevel,
            String status,
            Boolean enabled,
            Integer sortOrder
    ) {}

    public record ResourceItem(
            String categorySlug,
            String majorCode,
            String routeTitle,
            String title,
            String description,
            String icon,
            String resourceType,
            Integer requiredLevel,
            String status,
            Boolean enabled,
            String targetUrl,
            String originalName,
            String storageKey,
            Integer sortOrder
    ) {}

    public record ImportSummary(int professionalCategories, int specificMajors,
                                int learningRoutes, int learningResources) {}
}
