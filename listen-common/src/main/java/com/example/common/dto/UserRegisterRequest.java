package com.example.common.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class UserRegisterRequest {
    @NotEmpty(message = "openId不能为空")
    private String openId;

    @NotEmpty(message = "姓名不能为空")
    private String name;

    private Integer hospitalId;

    private String number;

    private String medicalId;

    private Integer age;
}
