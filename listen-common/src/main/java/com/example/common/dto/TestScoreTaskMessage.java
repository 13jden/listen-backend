package com.example.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestScoreTaskMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String testDetailId;
    private String userContent;
    private String audioContent;
    private String audioId;
    private String userAudioPath;
    private Float userDuration;
    private Float standardDuration;
    /**
     * 原始音频文件路径（转码前），不为空表示需要先进行音频处理
     */
    private String rawAudioPath;
}
