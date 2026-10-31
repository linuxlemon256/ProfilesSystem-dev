package com.example.profilessystem.server;

import com.example.profilessystem.entity.LearningResource;
import com.example.profilessystem.mapper.LearningResourceMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LearningResourceService {
    private final LearningResourceMapper mapper;
    private final LearningAccessService accessService;

    public LearningResourceService(LearningResourceMapper mapper, LearningAccessService accessService) {
        this.mapper = mapper;
        this.accessService = accessService;
    }

    public List<LearningResource> find(String category, String major, String query, String username) {
        return mapper.findVisible(clean(category), clean(major), clean(query), accessService.level(username));
    }

    public LearningResource findById(long id, String username) {
        return mapper.findVisibleById(id, accessService.level(username));
    }

    public List<LearningResource> findAllForAdmin() { return mapper.findAllForAdmin(); }

    public LearningResource create(LearningResource resource) {
        normalize(resource);
        mapper.insert(resource);
        return resource;
    }

    public boolean update(long id, LearningResource resource) {
        resource.setId(id);
        normalize(resource);
        return mapper.update(resource) == 1;
    }

    public boolean delete(long id) { return mapper.delete(id) == 1; }

    private String clean(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private void normalize(LearningResource resource) {
        if (resource.getTitle() == null || resource.getTitle().isBlank()) throw new IllegalArgumentException("资源标题不能为空");
        if (resource.getCategoryId() == null) throw new IllegalArgumentException("资源必须关联专业类别");
        resource.setTitle(resource.getTitle().trim());
        resource.setDescription(resource.getDescription() == null ? "" : resource.getDescription().trim());
        resource.setIcon(resource.getIcon() == null || resource.getIcon().isBlank() ? "✦" : resource.getIcon().trim());
        resource.setResourceType(resource.getResourceType() == null || resource.getResourceType().isBlank() ? "LINK" : resource.getResourceType().trim().toUpperCase());
        resource.setStatus(resource.getStatus() == null || resource.getStatus().isBlank() ? "DRAFT" : resource.getStatus().trim().toUpperCase());
        resource.setRequiredLevel(Math.max(0, Math.min(2, resource.getRequiredLevel())));
    }
}
