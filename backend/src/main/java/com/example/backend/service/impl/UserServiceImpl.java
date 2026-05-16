package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.constant.RedisConstants;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.LoginDTO;
import com.example.backend.entity.User;
import com.example.backend.mapper.UserMapper;
import com.example.backend.service.UserService;
import com.example.backend.util.JwtUtil;
import com.example.backend.util.RedisUtil;
import com.example.backend.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        // 1. 参数校验
        if (loginDTO.getUsername() == null || loginDTO.getUsername().trim().isEmpty()) {
            throw new BusinessException("用户名不能为空");
        }
        if (loginDTO.getPassword() == null || loginDTO.getPassword().trim().isEmpty()) {
            throw new BusinessException("密码不能为空");
        }

        // 2. 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // 3. 验证密码（暂时明文比对，后期需要加密）
        if (!user.getPassword().equals(loginDTO.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 4. 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        // 5. 生成Token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 6. 将Token存入Redis
        String redisKey = RedisConstants.TOKEN_PREFIX + token;
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("role", user.getRole());
        userInfo.put("realName", user.getRealName());
        redisUtil.set(redisKey, userInfo, RedisConstants.TOKEN_EXPIRE_TIME, TimeUnit.SECONDS);

        // 7. 封装返回结果
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUserId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setRealName(user.getRealName());
        loginVO.setRole(user.getRole());

        return loginVO;
    }

    @Override
    public User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 清空密码字段
        user.setPassword(null);
        return user;
    }

    @Override
    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }
}
