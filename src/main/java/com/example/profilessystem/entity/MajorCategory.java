package com.example.profilessystem.entity;

public class MajorCategory {
    private Long id;
    private String slug;
    private String name;
    private String fieldName;
    private String summary;
    private String icon;
    private int featured;
    private int enabled;
    private int sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public int getFeatured() { return featured; }
    public void setFeatured(int featured) { this.featured = featured; }
    public int getEnabled() { return enabled; }
    public void setEnabled(int enabled) { this.enabled = enabled; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
