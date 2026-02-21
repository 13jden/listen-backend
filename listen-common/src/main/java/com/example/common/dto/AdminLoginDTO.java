package com.example.common.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class AdminLoginDTO {

    @NotEmpty(message = "用户名不能为空")
    private String name;

    @NotEmpty(message = "密码不能为空")
    private String password;

    @NotEmpty(message = "验证码Key不能为空")
    private String checkCodeKey;

    @NotEmpty(message = "验证码不能为空")
    private String checkCode;
}
