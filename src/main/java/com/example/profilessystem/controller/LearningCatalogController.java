package com.example.profilessystem.controller;

import com.example.profilessystem.auth.SessionKeys;
import com.example.profilessystem.entity.LearningCategoryPage;
import com.example.profilessystem.entity.MajorCategory;
import com.example.profilessystem.result.Result;
import com.example.profilessystem.server.LearningCatalogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/learning")
public class LearningCatalogController {
    private final LearningCatalogService service;

    public LearningCatalogController(LearningCatalogService service) { this.service = service; }

    @GetMapping("/categories")
    public Result<List<MajorCategory>> categories() { return Result.ok(service.categories()); }

    @GetMapping("/categories/{slug}")
    public Result<LearningCategoryPage> category(@PathVariable String slug, HttpSession session) {
        LearningCategoryPage page = service.category(slug, username(session));
        return page == null ? Result.fail(404, "专业类别不存在") : Result.ok(page);
    }

    private String username(HttpSession session) {
        Object value = session.getAttribute(SessionKeys.USER);
        return value == null ? null : value.toString();
    }
}
