package com.example.profilessystem.controller;

import com.example.profilessystem.entity.LearningResource;
import com.example.profilessystem.result.Result;
import com.example.profilessystem.server.LearningResourceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/learning")
public class LearningAdminController {
    private final LearningResourceService resourceService;

    public LearningAdminController(LearningResourceService resourceService) { this.resourceService = resourceService; }

    @GetMapping("/resources")
    public Result<List<LearningResource>> resources() { return Result.ok(resourceService.findAllForAdmin()); }

    @PostMapping("/resources")
    public Result<LearningResource> create(@RequestBody LearningResource resource) {
        return Result.ok(resourceService.create(resource));
    }

    @PutMapping("/resources/{id}")
    public Result<Void> update(@PathVariable long id, @RequestBody LearningResource resource) {
        return resourceService.update(id, resource) ? Result.ok() : Result.fail(404, "资源不存在");
    }

    @DeleteMapping("/resources/{id}")
    public Result<Void> delete(@PathVariable long id) {
        return resourceService.delete(id) ? Result.ok() : Result.fail(404, "资源不存在");
    }
}
