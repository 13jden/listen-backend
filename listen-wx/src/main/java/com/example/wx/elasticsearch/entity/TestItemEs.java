package com.example.wx.elasticsearch.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.DateFormat;

import java.time.LocalDateTime;

/**
 * ES单题明细索引 - 对应 test_items 索引
 * 存储每道题的详细数据，用于错误分析、样本追溯
 * 通过 testId 与 TestEs 关联
 */
@Data
@Document(indexName = "test_items", createIndex = false)
public class TestItemEs {

    /**
     * 测试详情ID（对应 testdetail 表的 id）
     */
    @Id
    @Field(type = FieldType.Keyword)
    private String id;

    /**
     * 测试ID（对应 usertest 表的 id，外键关联 TestEs）
     */
    @Field(type = FieldType.Keyword)
    private String testId;

    /**
     * 题目序号（第几题）
     */
    @Field(type = FieldType.Integer)
    private Integer itemIndex;

    /**
     * 音频ID（对应 audio 表的 id，用于关联错误音频）
     */
    @Field(type = FieldType.Keyword)
    private String audioId;


    /**
     * 标准文本（正确答案）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String standardText;

    /**
     * 用户ASR识别文本
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String userAsrText;

    /**
     * 用户语音时长（秒）
     */
    @Field(type = FieldType.Float)
    private Float speechDurationSec;

    /**
     * 标准音频时长（秒）
     */
    @Field(type = FieldType.Float)
    private Float standardDurationSec;

    /**
     * 时长得分（基于用户时长与标准时长的比例）
     */
    @Field(type = FieldType.Float)
    private Float durationScore;

    /**
     * 编辑距离（Levenshtein距离）
     */
    @Field(type = FieldType.Integer)
    private Integer editDistance;

    /**
     * 文本相似度
     */
    @Field(type = FieldType.Float)
    private Float textSimilarity;

    /**
     * LLM评分
     */
    @Field(type = FieldType.Float)
    private Float llmScore;

    /**
     * 最终得分
     */
    @Field(type = FieldType.Float)
    private Float finalScore;

    /**
     * 是否正确（>=60分为正确）
     */
    @Field(type = FieldType.Boolean)
    private Boolean isCorrect;

    /**
     * 错误标签数组（如 ["平翘舌", "前后鼻音"]）
     */
    @Field(type = FieldType.Keyword)
    private String[] errorTags;

    /**
     * 错误详情（错误位置等信息）
     */
    @Field(type = FieldType.Text)
    private String errorDetail;

    /**
     * 结果分析
     */
    @Field(type = FieldType.Text)
    private String resultAnalysis;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;
}
