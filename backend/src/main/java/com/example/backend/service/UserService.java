package com.example.backend.service;

import com.example.backend.dto.LoginDTO;
import com.example.backend.entity.User;
import com.example.backend.vo.LoginVO;

public interface UserService {

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 根据ID获取用户信息
     */
    User getUserById(Long userId);

    /**
     * 根据用户名获取用户
     */
    User getUserByUsername(String username);
}
