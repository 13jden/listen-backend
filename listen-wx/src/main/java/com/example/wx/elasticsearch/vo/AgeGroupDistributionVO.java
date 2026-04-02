package com.example.wx.elasticsearch.vo;

import lombok.Data;
import java.util.List;

/**
 * 年龄段分布VO
 */
@Data
public class AgeGroupDistributionVO {
    private List<AgeGroupItem> data;

    @Data
    public static class AgeGroupItem {
        private String ageGroup;     // 年龄段名称
        private Double maxScore;     // 最高分
        private Double minScore;     // 最低分
        private Double avgScore;     // 平均得分
        private Long count;          // 测试人数
    }
}
