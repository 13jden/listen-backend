package com.example.wx.elasticsearch.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * ES单题明细索引 - 对应 test_items 索引
 * 存储每道题的详细数据，用于错误分析、样本追溯
 * 通过 testId 与 TestEs 关联
 */
@Data
public class TestItemEs {

    /**
     * 测试详情ID（对应 testdetail 表的 id）
     */
    private String id;

    /**
     * 测试ID（对应 usertest 表的 id，外键关联 TestEs）
     */
    private String testId;

    /**
     * 题目序号（第几题）
     */
    private Integer itemIndex;

    /**
     * 音频ID（对应 audio 表的 id，用于关联错误音频）
     */
    private String audioId;


    /**
     * 标准文本（正确答案）
     */
    private String standardText;

    /**
     * 用户ASR识别文本
     */
    private String userAsrText;

    /**
     * 用户语音时长（秒）
     */
    private Float speechDurationSec;

    /**
     * 标准音频时长（秒）
     */
    private Float standardDurationSec;

    /**
     * 时长得分（基于用户时长与标准时长的比例）
     */
    private Float durationScore;

    /**
     * 编辑距离（Levenshtein距离）
     */
    private Integer editDistance;

    /**
     * 文本相似度
     */
    private Float textSimilarity;

    /**
     * LLM评分
     */
    private Float llmScore;

    /**
     * 最终得分
     */
    private Float finalScore;

    /**
     * 是否正确（>=60分为正确）
     */
    private Boolean isCorrect;

    /**
     * 错误标签数组（如 ["平翘舌", "前后鼻音"]）
     */
    private String[] errorTags;

    /**
     * 错误详情（错误位置等信息）
     */
    private String errorDetail;

    /**
     * 结果分析
     */
    private String resultAnalysis;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
