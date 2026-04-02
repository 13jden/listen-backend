package com.example.wx.elasticsearch.vo;

import lombok.Data;
import java.util.List;

/**
 * 医院测试统计VO
 */
@Data
public class HospitalStatsVO {
    private List<HospitalItem> data;

    @Data
    public static class HospitalItem {
        private String hospitalName;  // 医院名称
        private Long userCount;        // 测试人数
        private Long testCount;        // 测试次数
        private Double avgScore;       // 平均得分
    }
}
