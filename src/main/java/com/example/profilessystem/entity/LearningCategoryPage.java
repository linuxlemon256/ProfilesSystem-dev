package com.example.profilessystem.entity;

import java.util.List;

public class LearningCategoryPage {
    private MajorCategory category;
    private List<Major> majors;
    private List<LearningRoute> routes;

    public LearningCategoryPage(MajorCategory category, List<Major> majors, List<LearningRoute> routes) {
        this.category = category;
        this.majors = majors;
        this.routes = routes;
    }

    public MajorCategory getCategory() { return category; }
    public void setCategory(MajorCategory category) { this.category = category; }
    public List<Major> getMajors() { return majors; }
    public void setMajors(List<Major> majors) { this.majors = majors; }
    public List<LearningRoute> getRoutes() { return routes; }
    public void setRoutes(List<LearningRoute> routes) { this.routes = routes; }
}
