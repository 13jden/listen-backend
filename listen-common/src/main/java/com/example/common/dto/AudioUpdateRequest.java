package com.example.common.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class AudioUpdateRequest {
    @NotEmpty(message = "音频ID不能为空")
    private String id;

    private String content;

    private String path;
}
