package com.example.profilessystem.server;

import com.example.profilessystem.entity.LearningCategoryPage;
import com.example.profilessystem.entity.MajorCategory;
import com.example.profilessystem.mapper.LearningCatalogMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LearningCatalogService {
    private final LearningCatalogMapper mapper;
    private final LearningAccessService accessService;

    public LearningCatalogService(LearningCatalogMapper mapper, LearningAccessService accessService) {
        this.mapper = mapper;
        this.accessService = accessService;
    }

    public List<MajorCategory> categories() {
        return mapper.findCategories();
    }

    public LearningCategoryPage category(String slug, String username) {
        MajorCategory category = mapper.findCategoryBySlug(slug);
        if (category == null) return null;
        return new LearningCategoryPage(
                category,
                mapper.findMajors(category.getId()),
                mapper.findRoutes(category.getId(), accessService.level(username)));
    }
}
