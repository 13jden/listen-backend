package com.example.wx.elasticsearch.vo;

import lombok.Data;
import java.util.List;

/**
 * 得分分布VO
 */
@Data
public class ScoreDistributionVO {
    private List<String> ranges;      // 分数段
    private List<Long> counts;        // 人数
}
