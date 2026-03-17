package com.example.wx.elasticsearch.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.DateFormat;

import java.time.LocalDateTime;

/**
 * ES测试索引 - 对应 tests 索引
 * 存储单次用户测试的完整数据，包含用户基础信息、分数、错误信息等
 */
@Data
@Document(indexName = "tests", createIndex = false)
public class TestEs {

    /**
     * 测试ID（对应 usertest 表的 id）
     */
    @Id
    @Field(type = FieldType.Keyword)
    private String testId;

    /**
     * 用户ID（对应 user 表的 user_id）
     */
    @Field(type = FieldType.Keyword)
    private String userId;

    /**
     * 就诊卡号/医疗卡号
     */
    @Field(type = FieldType.Keyword)
    private String medicalId;

    /**
     * 医院ID
     */
    @Field(type = FieldType.Keyword)
    private String hospitalId;

    /**
     * 医院名称
     */
    @Field(type = FieldType.Keyword)
    private String hospitalName;

    /**
     * 年龄（具体数字）
     */
    @Field(type = FieldType.Integer)
    private Integer age;

    /**
     * 性别（男/女）
     */
    @Field(type = FieldType.Keyword)
    private String sex;

    /**
     * 测试开始时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime startTime;

    /**
     * 测试结束时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime endTime;

    /**
     * 测试时长（秒）
     */
    @Field(type = FieldType.Integer)
    private Integer durationSec;

    /**
     * 测试日期（yyyy-MM-dd 格式）
     */
    @Field(type = FieldType.Keyword)
    private String testDate;

    /**
     * 测试月份（yyyy-MM 格式）
     */
    @Field(type = FieldType.Keyword)
    private String testMonth;

    /**
     * 完成状态（completed/in_progress）
     */
    @Field(type = FieldType.Keyword)
    private String completionStatus;

    /**
     * 总分/平均分
     */
    @Field(type = FieldType.Float)
    private Double totalScore;

    /**
     * 是否及格（>=60分为及格）
     */
    @Field(type = FieldType.Boolean)
    private Boolean passFlag;

    /**
     * 完成题目数量
     */
    @Field(type = FieldType.Integer)
    private Integer itemCount;

    /**
     * 总题目数量
     */
    @Field(type = FieldType.Integer)
    private Integer totalItems;


    /**
     * 结果分析（总体评估和建议）
     */
    @Field(type = FieldType.Text)
    private String resultAnalysis;
}
