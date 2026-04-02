package com.example.wx.elasticsearch.vo;

import lombok.Data;
import java.util.List;

/**
 * 每日数据报表VO
 */
@Data
public class DailyReportVO {
    private List<DailyReportItem> data;

    @Data
    public static class DailyReportItem {
        private String date;           // 日期
        private Long userCount;        // 总测试人数
        private Long testCount;        // 测试次数
        private Double avgScore;       // 平均分
        private Double completionRate; // 完成率
    }
}
