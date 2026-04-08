package com.example.common.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class AudioUploadRequest {
    @NotEmpty(message = "音频文件不能为空")
    private String fileName;

    private Float durationSec;
}
