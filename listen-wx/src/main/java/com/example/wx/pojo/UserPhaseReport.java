package com.example.wx.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户阶段性报告实体
 */
@Data
@TableName("user_phase_report")
public class UserPhaseReport implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 报告ID
     */
    @TableId(type = IdType.INPUT)
    private String id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 总测试次数
     */
    private Integer totalTestCount;

    /**
     * 平均得分
     */
    private BigDecimal avgScore;

    /**
     * 得分趋势（JSON数组格式，如 [85.5, 87.0, 88.5]）
     */
    private String scoreTrend;

    /**
     * 测试分析（AI生成）
     */
    private String testAnalysis;

    /**
     * 医生评语
     */
    private String doctorComment;

    /**
     * 医生建议
     */
    private String doctorSuggestion;

    /**
     * 生成时间
     */
    private Date reportDate;

    /**
     * 创建时间
     */
    @TableField(select = false)
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
