package com.example.backend.controller;

import com.example.backend.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    /**
     * 测试接口
     */
    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("ColdChain System is running!");
    }

    /**
     * 测试返回数据
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        Map<String, Object> data = new HashMap<>();
        data.put("projectName", "冷链智能调度系统");
        data.put("version", "1.0.0");
        data.put("author", "大创项目");
        data.put("time", LocalDateTime.now());
        return Result.success(data);
    }
}
