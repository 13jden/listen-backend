package com.example.wx.elasticsearch.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ES测试索引 - 对应 tests 索引
 * 存储单次用户测试的完整数据，包含用户基础信息、分数、错误信息等
 */
@Data
public class TestEs {

    /**
     * 测试ID（对应 usertest 表的 id）
     */
    private String testId;

    /**
     * 用户ID（对应 user 表的 user_id）
     */
    private String userId;

    /**
     * 就诊卡号/医疗卡号
     */
    private String medicalId;

    /**
     * 医院ID
     */
    private String hospitalId;

    /**
     * 医院名称
     */
    private String hospitalName;

    /**
     * 年龄（具体数字）
     */
    private Integer age;

    /**
     * 性别（男/女）
     */
    private String sex;

    /**
     * 测试开始时间（与 ES date 映射含毫秒格式一致）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime startTime;

    /**
     * 测试结束时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime endTime;

    /**
     * 测试时长（秒）
     */
    private Integer durationSec;

    /**
     * 测试日期（yyyy-MM-dd 格式）
     */
    private String testDate;

    /**
     * 测试月份（yyyy-MM 格式）
     */
    private String testMonth;

    /**
     * 完成状态（completed/in_progress）
     */
    private String completionStatus;

    /**
     * 总分/平均分
     */
    private Double totalScore;

    /**
     * 是否及格（>=60分为及格）
     */
    private Boolean passFlag;

    /**
     * 完成题目数量
     */
    private Integer itemCount;

    /**
     * 总题目数量
     */
    private Integer totalItems;

    /**
     * 结果分析（总体评估和建议）
     */
    private String resultAnalysis;
}
