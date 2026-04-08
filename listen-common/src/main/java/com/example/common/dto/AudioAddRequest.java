package com.example.common.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class AudioAddRequest {
    @NotEmpty(message = "内容不能为空")
    private String content;

    @NotEmpty(message = "管理员ID不能为空")
    private String adminId;

    @NotEmpty(message = "文件名不能为空")
    private String fileName;

    private Float durationSec;
}
