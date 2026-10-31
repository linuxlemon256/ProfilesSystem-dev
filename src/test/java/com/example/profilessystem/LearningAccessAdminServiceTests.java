package com.example.profilessystem;

import com.example.profilessystem.entity.LearningAccessTree;
import com.example.profilessystem.server.LearningAccessAdminService;
import com.example.profilessystem.server.LearningCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@Transactional
class LearningAccessAdminServiceTests {
    @Autowired
    private LearningAccessAdminService service;
    @Autowired
    private LearningCatalogService catalogService;

    @Test
    void categoryAndChildrenCanBeControlledWithoutOverwritingEachOther() {
        LearningAccessTree.CategoryNode category = service.tree().categories().stream()
                .filter(item -> !item.specificMajors().isEmpty() && !item.learningRoutes().isEmpty())
                .findFirst()
                .orElseThrow();
        LearningAccessTree.ContentNode major = category.specificMajors().get(0);
        LearningAccessTree.ContentNode route = category.learningRoutes().get(0);

        service.setEnabled("category", category.id(), true);
        service.setEnabled("category", category.id(), false);
        assertFalse(catalogService.categories().stream().anyMatch(item -> item.getId().equals(category.id())));

        service.setEnabled("category", category.id(), true);
        service.setEnabled("major", major.id(), false);
        service.setEnabled("route", route.id(), false);

        LearningAccessTree.CategoryNode updated = service.tree().categories().stream()
                .filter(item -> item.id().equals(category.id()))
                .findFirst()
                .orElseThrow();
        assertEquals(true, updated.enabled());
        assertFalse(updated.specificMajors().stream().filter(item -> item.id().equals(major.id())).findFirst().orElseThrow().enabled());
        assertFalse(updated.learningRoutes().stream().filter(item -> item.id().equals(route.id())).findFirst().orElseThrow().enabled());
        assertFalse(catalogService.category(category.slug(), null).getMajors().stream()
                .anyMatch(item -> item.getId().equals(major.id())));
        assertFalse(catalogService.category(category.slug(), null).getRoutes().stream()
                .anyMatch(item -> item.getId().equals(route.id())));
    }
}
