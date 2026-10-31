package com.example.profilessystem.entity;

import java.util.List;

public record LearningAccessTree(List<CategoryNode> categories) {
    public record CategoryNode(
            Long id,
            String slug,
            String name,
            boolean enabled,
            List<ContentNode> specificMajors,
            List<ContentNode> learningRoutes,
            List<ContentNode> learningResources
    ) {}

    public record ContentNode(
            Long id,
            String name,
            String detail,
            boolean enabled
    ) {}

    public record UpdateBody(Boolean enabled) {}

    public record UpdateResult(String contentType, long id, boolean enabled) {}
}
