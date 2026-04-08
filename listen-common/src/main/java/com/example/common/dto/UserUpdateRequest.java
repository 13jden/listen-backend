package com.example.common.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class UserUpdateRequest {
    @NotEmpty(message = "用户ID不能为空")
    private String userId;

    private String name;

    private Integer hospitalId;

    private String number;
}
