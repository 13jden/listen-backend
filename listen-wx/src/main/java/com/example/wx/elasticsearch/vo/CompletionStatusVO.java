package com.example.wx.elasticsearch.vo;

import lombok.Data;

/**
 * 测试完成状态VO
 */
@Data
public class CompletionStatusVO {
    private Long completed;    // 已完成数
    private Long inProgress;  // 进行中数
    private Long notStarted;  // 未开始数
}
