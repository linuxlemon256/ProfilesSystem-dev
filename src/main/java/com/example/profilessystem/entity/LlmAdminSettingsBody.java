package com.example.profilessystem.entity;

public class LlmAdminSettingsBody {
    private Integer level0Tokens;
    private Integer level1Tokens;
    private Integer level2Tokens;
    private Integer refreshIntervalHours;
    private String modelName;
    private String baseUrl;
    private String apiKey;
    public Integer getLevel0Tokens(){return level0Tokens;} public void setLevel0Tokens(Integer v){level0Tokens=v;}
    public Integer getLevel1Tokens(){return level1Tokens;} public void setLevel1Tokens(Integer v){level1Tokens=v;}
    public Integer getLevel2Tokens(){return level2Tokens;} public void setLevel2Tokens(Integer v){level2Tokens=v;}
    public Integer getRefreshIntervalHours(){return refreshIntervalHours;} public void setRefreshIntervalHours(Integer v){refreshIntervalHours=v;}
    public String getModelName(){return modelName;} public void setModelName(String v){modelName=v;}
    public String getBaseUrl(){return baseUrl;} public void setBaseUrl(String v){baseUrl=v;}
    public String getApiKey(){return apiKey;} public void setApiKey(String v){apiKey=v;}
}
