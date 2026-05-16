package com.example.backend.vo;

import lombok.Data;
//返回对象
@Data
public class LoginVO {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String role;
}
