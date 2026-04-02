package com.example.wx.elasticsearch.vo;

import lombok.Data;
import java.util.List;

/**
 * 月度测试趋势VO
 */
@Data
public class MonthlyTrendVO {
    private List<String> months;       // 月份列表
    private List<Long> testCounts;    // 测试次数
    private List<Double> avgScores;   // 平均得分
}
