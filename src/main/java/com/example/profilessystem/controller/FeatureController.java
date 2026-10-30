package com.example.profilessystem.controller;

import com.example.profilessystem.entity.FeatureToggleBody;
import com.example.profilessystem.mapper.FeatureMapper;
import com.example.profilessystem.result.Result;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/features")
public class FeatureController {
    private final FeatureMapper mapper;
    public FeatureController(FeatureMapper mapper){this.mapper=mapper;}
    @GetMapping public Result<List<Map<String,Object>>> all(){return Result.ok(mapper.all());}
    @PostMapping("/{feature}") public Result<String> save(@PathVariable String feature,@RequestBody FeatureToggleBody body){if(body==null||body.getEnabled()==null)return Result.fail(Result.CODE_BAD_REQUEST,"开关状态不能为空");return mapper.save(feature,body.getEnabled()?1:0,body.getReason()==null?"":body.getReason().trim())>0?Result.ok(null):Result.fail("保存失败");}
}
