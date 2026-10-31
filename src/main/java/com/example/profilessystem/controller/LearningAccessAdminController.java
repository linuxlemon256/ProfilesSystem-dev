package com.example.profilessystem.controller;

import com.example.profilessystem.entity.LearningAccessTree;
import com.example.profilessystem.result.Result;
import com.example.profilessystem.server.LearningAccessAdminService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/learning-access")
public class LearningAccessAdminController {
    private final LearningAccessAdminService service;

    public LearningAccessAdminController(LearningAccessAdminService service) {
        this.service = service;
    }

    @GetMapping
    public Result<LearningAccessTree> tree() {
        return Result.ok(service.tree());
    }

    @PutMapping("/{contentType}/{id}")
    public Result<LearningAccessTree.UpdateResult> setEnabled(
            @PathVariable String contentType,
            @PathVariable long id,
            @RequestBody LearningAccessTree.UpdateBody body) {
        if (body == null || body.enabled() == null) {
            return Result.fail(400, "必须指定开放或关闭状态");
        }
        return Result.ok(service.setEnabled(contentType, id, body.enabled()));
    }
}
