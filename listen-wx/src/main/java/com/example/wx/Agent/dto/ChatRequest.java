package com.example.wx.Agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "AI对话请求")
public class ChatRequest {

    @Schema(description = "用户问题")
    private String content;

    @Schema(description = "对话历史")
    private List<Memory> history;

    @Schema(description = "用户测试信息")
    private List<TestInfo> testInfo;

    @Data
    @Schema(description = "对话历史记录")
    public static class Memory {
        @Schema(description = "用户消息")
        private String user;
        @Schema(description = "AI回复")
        private String assistant;
    }

    @Data
    @Schema(description = "用户测试信息")
    public static class TestInfo {
        @Schema(description = "用户ID")
        private String userId;
        @Schema(description = "测试ID")
        private String testId;
        @Schema(description = "测试时间")
        private String testTime;
        @Schema(description = "测试项目（如：听说能力测试）")
        private String testType;
        @Schema(description = "平均得分")
        private Float avgScore;
        @Schema(description = "测试题目数")
        private Integer totalItems;
        @Schema(description = "错误标签汇总（如：平翘舌,前后鼻音）")
        private String errorTags;
        @Schema(description = "详细分析")
        private String analysis;
    }
}
