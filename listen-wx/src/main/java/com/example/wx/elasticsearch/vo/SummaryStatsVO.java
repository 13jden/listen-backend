package com.example.wx.elasticsearch.vo;

import lombok.Data;

/**
 * 统计总计VO
 */
@Data
public class SummaryStatsVO {
    private Long totalUsers;           // 总用户数
    private Long totalTests;          // 总测试数
    private Double avgScore;           // 平均得分
    private Double scoreImprovement;  // 每日分数提升幅度
}
