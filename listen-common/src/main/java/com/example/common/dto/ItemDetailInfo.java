package com.example.common.dto;

import lombok.Data;

import java.util.Date;

/**
 * 测试题目详情信息DTO
 * 用于管理端获取单条测试题目的详细信息
 */
@Data
public class ItemDetailInfo {

    /**
     * 测试详情ID（对应testdetail表的主键）
     */
    private String testDetailId;

    /**
     * 测试ID（对应usertest表的主键，外键关联）
     */
    private String testId;

    /**
     * 题目序号（第几题）
     */
    private Integer index;

    /**
     * 音频ID（对应audio表的主键）
     */
    private String audioId;

    /**
     * 标准音频URL路径
     */
    private String audioPath;

    /**
     * 用户录音URL路径
     */
    private String userAudioPath;

    /**
     * 标准音频文本内容（正确答案）
     */
    private String audioContent;

    /**
     * 用户ASR语音识别文本
     */
    private String userContent;

    /**
     * 最终得分（编辑距离×0.5 + AI评分×0.3 + 时长得分×0.2，满分100）
     */
    private Float score;

    /**
     * 时长得分（基于用户时长与标准时长的比例计算，满分100）
     */
    private Float durationScore;

    /**
     * 编辑距离得分（基于Levenshtein距离计算文本相似度，满分100）
     */
    private Float editDistanceScore;

    /**
     * AI评分得分（由大模型评分，满分100）
     */
    private Float aiScore;

    /**
     * 用户语音时长（秒）
     */
    private Float speechDurationSec;

    /**
     * 标准音频时长（秒）
     */
    private Float standardDurationSec;

    /**
     * 错误位置（用户发音与标准文本不匹配的位置，用逗号分隔，如"3,5,8"）
     */
    private String errorPositions;

    /**
     * 错误标签（发音错误类型，如"平翘舌,前后鼻音"）
     */
    private String errorTags;

    /**
     * 结果分析（包含错误对比和详细分析）
     */
    private String resultAnalysis;

    /**
     * 测试时间
     */
    private Date time;
}
