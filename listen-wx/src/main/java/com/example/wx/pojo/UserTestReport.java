package com.example.wx.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户测试报告实体
 */
@Data
@TableName("user_test_report")
public class UserTestReport implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 报告ID（雪花算法生成）
     */
    @TableId(type = IdType.INPUT)
    private String id;

    /**
     * 测试ID（关联usertest表）
     */
    private String testId;

    /**
     * 平均得分（所有题目得分的平均值）
     */
    private BigDecimal avgScore;

    /**
     * 总题目数
     */
    private Integer totalItems;

    /**
     * 正确数（得分>=60分的题目数量）
     */
    private Integer correctCount;

    /**
     * 错误数（得分<60分的题目数量）
     */
    private Integer errorCount;

    /**
     * 错误标签汇总（JSON数组格式，如["平翘舌","前后鼻音"]）
     */
    private String errorTags;

    /**
     * 错误汇总描述（包含具体错误题目的对比信息）
     */
    private String errorSummary;

    /**
     * 改善建议（由AI生成的针对性训练建议）
     */
    private String improvementSuggestion;

    /**
     * AI综合分析结果（整体表现评价）
     */
    private String aiAnalysis;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
