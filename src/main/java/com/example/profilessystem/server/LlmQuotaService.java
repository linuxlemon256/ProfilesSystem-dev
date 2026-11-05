package com.example.profilessystem.server;

import com.example.profilessystem.entity.LlmAdminSettingsBody;
import com.example.profilessystem.mapper.LlmMapper;
import com.example.profilessystem.mapper.UserMapper;
import com.example.profilessystem.result.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class LlmQuotaService {
    private final LlmMapper mapper; private final UserMapper users;
    @Value("${LLM_API_KEY:}") private String envApiKey;
    @Value("${LLM_BASE_URL:}") private String envBaseUrl;
    @Value("${LLM_MODEL:deepseek-flash}") private String envModel;
    @Value("${LLM_PROVIDER:openapi}") private String envProvider;
    @Value("${OLLAMA_BASE_URL:http://127.0.0.1:11434}") private String localOllamaBaseUrl;
    public LlmQuotaService(LlmMapper mapper, UserMapper users){this.mapper=mapper;this.users=users;}
    private int settingInt(String key,int fallback){try{String v=mapper.getSetting(key);return v==null?fallback:Math.max(0,Integer.parseInt(v));}catch(Exception e){return fallback;}}
    private String setting(String key,String fallback){String v=mapper.getSetting(key);return v==null||v.isBlank()?fallback:v;}
    public String provider(){String value=setting("provider",envProvider).toLowerCase();return "ollama".equals(value)||"ollama_external".equals(value)?value:"openapi";}
    public String modelName(){return setting("model_name",envModel);}
    public String baseUrl(){return "ollama".equals(provider())?localOllamaBaseUrl:setting("base_url",envBaseUrl);}
    public String apiKey(){return setting("api_key",envApiKey);}
    public boolean modelConfigured(){String url=baseUrl();String model=modelName();return url!=null&&!url.isBlank()&&model!=null&&!model.isBlank()&&(provider().startsWith("ollama")||(apiKey()!=null&&!apiKey().isBlank()));}
    private int quota(int level){return settingInt("level"+Math.max(0,Math.min(2,level))+"_tokens", level==2?20000:level==1?8000:2000);}
    @Transactional
    public Result<Map<String,Object>> consume(String username,int requestedTokens){
        var user=users.findByName(username); if(user==null)return Result.fail(Result.CODE_UNAUTHORIZED,"账户不存在");
        int interval=settingInt("refresh_interval_hours",24); LocalDateTime refreshed=mapper.refreshedAt(username);
        if(refreshed!=null && refreshed.plusHours(interval).isBefore(LocalDateTime.now())) mapper.resetUser(username);
        int used=mapper.usedTokens(username)==null?0:mapper.usedTokens(username); int limit=quota(user.getLevel());
        if(used+requestedTokens>limit){Map<String,Object> data=quotaData(user.getLevel(),used,limit);return new Result<>(429,"今日 token 额度不足",data,java.time.Instant.now());}
        mapper.addTokens(username,requestedTokens); return Result.ok(quotaData(user.getLevel(),used+requestedTokens,limit));
    }
    public Map<String,Object> quotaData(int level,int used,int limit){Map<String,Object> m=new LinkedHashMap<>();m.put("level",level);m.put("usedTokens",used);m.put("quotaTokens",limit);m.put("remainingTokens",Math.max(0,limit-used));m.put("refreshIntervalHours",settingInt("refresh_interval_hours",24));return m;}
    public Result<Map<String,Object>> status(String username){var u=users.findByName(username);if(u==null)return Result.fail(Result.CODE_UNAUTHORIZED,"账户不存在");int interval=settingInt("refresh_interval_hours",24);LocalDateTime r=mapper.refreshedAt(username);if(r!=null&&r.plusHours(interval).isBefore(LocalDateTime.now())){mapper.resetUser(username);r=null;}int used=mapper.usedTokens(username)==null?0:mapper.usedTokens(username);Map<String,Object> data=quotaData(u.getLevel(),used,quota(u.getLevel()));data.put("refreshedAt",r);return Result.ok(data);}
    public Result<Map<String,Object>> adminSettings(){Map<String,Object> m=new LinkedHashMap<>();m.put("level0Tokens",settingInt("level0_tokens",2000));m.put("level1Tokens",settingInt("level1_tokens",8000));m.put("level2Tokens",settingInt("level2_tokens",20000));m.put("refreshIntervalHours",settingInt("refresh_interval_hours",24));m.put("provider",provider());m.put("modelName",modelName());m.put("baseUrl",baseUrl());String key=apiKey();m.put("apiKeyConfigured",key!=null&&!key.isBlank());m.put("configured",modelConfigured());return Result.ok(m);}
    @Transactional public Result<String> saveAdminSettings(LlmAdminSettingsBody b){
        if(b==null)
            return Result.fail(Result.CODE_BAD_REQUEST,"配置不能为空");
        if(b.getLevel0Tokens()==null||b.getLevel1Tokens()==null||b.getLevel2Tokens()==null||b.getRefreshIntervalHours()==null||b.getRefreshIntervalHours()<1)
            return Result.fail(Result.CODE_BAD_REQUEST,"额度和刷新间隔必须有效");
        String provider=b.getProvider()==null?"openapi":b.getProvider().trim().toLowerCase();
        if(!"openapi".equals(provider)&&!"ollama".equals(provider)&&!"ollama_external".equals(provider))
            return Result.fail(Result.CODE_BAD_REQUEST,"不支持的模型接入方式");
        if("ollama_external".equals(provider)&&(b.getBaseUrl()==null||b.getBaseUrl().isBlank()))
            return Result.fail(Result.CODE_BAD_REQUEST,"外接 Ollama 必须填写服务地址");
        mapper.saveSetting("level0_tokens",String.valueOf(b.getLevel0Tokens()));
        mapper.saveSetting("level1_tokens",String.valueOf(b.getLevel1Tokens()));
        mapper.saveSetting("level2_tokens",String.valueOf(b.getLevel2Tokens()));
        mapper.saveSetting("refresh_interval_hours",String.valueOf(b.getRefreshIntervalHours()));
        mapper.saveSetting("provider",provider);
        if(b.getModelName()!=null)
            mapper.saveSetting("model_name",b.getModelName().trim());
        if(!"ollama".equals(provider)&&b.getBaseUrl()!=null)
            mapper.saveSetting("base_url",b.getBaseUrl().trim());
        if(b.getApiKey()!=null&&!b.getApiKey().isBlank())
            mapper.saveSetting("api_key",b.getApiKey().trim());
        return Result.ok(null);}
    public Result<String> resetAll(){mapper.resetAll();return Result.ok(null);}
}
