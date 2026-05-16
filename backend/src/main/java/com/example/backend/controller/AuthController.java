package com.example.backend.controller;

import com.example.backend.common.constant.RedisConstants;
import com.example.backend.common.result.Result;
import com.example.backend.dto.LoginDTO;
import com.example.backend.entity.User;
import com.example.backend.service.UserService;
import com.example.backend.util.JwtUtil;
import com.example.backend.util.RedisUtil;
import com.example.backend.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = userService.login(loginDTO);
        return Result.success(loginVO);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/userinfo")
    public Result<User> getUserInfo(@RequestHeader("Authorization") String token) {
        // 去掉 "Bearer " 前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 1. 先从Redis验证Token是否存在
        String redisKey = RedisConstants.TOKEN_PREFIX + token;
        Object userInfoObj = redisUtil.get(redisKey);

        if (userInfoObj == null) {
            return Result.error("Token无效或已过期");
        }

        // 2. 从Redis获取用户信息
        Map<String, Object> userInfoMap = (Map<String, Object>) userInfoObj;
        Long userId = Long.valueOf(userInfoMap.get("userId").toString());

        // 3. 查询完整用户信息
        User user = userService.getUserById(userId);

        return Result.success(user);
    }

    /**
     * 测试接口 - 验证Token
     */
    @GetMapping("/test")
    public Result<String> test(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 从Redis验证Token
        String redisKey = RedisConstants.TOKEN_PREFIX + token;
        Object userInfoObj = redisUtil.get(redisKey);

        if (userInfoObj != null) {
            Map<String, Object> userInfoMap = (Map<String, Object>) userInfoObj;
            String username = userInfoMap.get("username").toString();
            return Result.success("Token有效，用户：" + username);
        } else {
            return Result.error("Token无效");
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 从Redis删除Token
        String redisKey = RedisConstants.TOKEN_PREFIX + token;
        redisUtil.delete(redisKey);

        return Result.success("登出成功");
    }
}
