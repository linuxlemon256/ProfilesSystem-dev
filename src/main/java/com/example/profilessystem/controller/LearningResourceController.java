package com.example.profilessystem.controller;

import com.example.profilessystem.auth.SessionKeys;
import com.example.profilessystem.entity.LearningResource;
import com.example.profilessystem.result.Result;
import com.example.profilessystem.server.LearningResourceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/learning/resources")
public class LearningResourceController {
    private final LearningResourceService service;

    public LearningResourceController(LearningResourceService service) { this.service = service; }

    @GetMapping
    public Result<List<LearningResource>> resources(@RequestParam(required = false) String category,
                                                     @RequestParam(required = false) String major,
                                                     @RequestParam(required = false, name = "q") String query,
                                                     HttpSession session) {
        return Result.ok(service.find(category, major, query, username(session)));
    }

    @GetMapping("/{id}")
    public Result<LearningResource> resource(@PathVariable long id, HttpSession session) {
        LearningResource resource = service.findById(id, username(session));
        return resource == null ? Result.fail(404, "资源不存在或当前等级无权访问") : Result.ok(resource);
    }

    private String username(HttpSession session) {
        Object value = session.getAttribute(SessionKeys.USER);
        return value == null ? null : value.toString();
    }
}
