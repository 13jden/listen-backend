package com.example.wx.elasticsearch.vo;

import lombok.Data;
import java.util.List;

/**
 * 错误类型分布VO
 */
@Data
public class ErrorTypeDistributionVO {
    private List<ErrorTypeItem> data;

    @Data
    public static class ErrorTypeItem {
        private String errorType;  // 错误类型
        private Long count;        // 错误次数
    }
}
