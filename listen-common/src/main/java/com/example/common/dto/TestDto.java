package com.example.common.dto;

import lombok.Data;

import java.util.Date;


@Data
public class TestDto {

    /**
     * 用户本次测试id
     */
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * 测试详细id
     */
    private String testId;
//
//    private String userId;

    /**
     * 用户的文件上传地址+name;有则为已测试
     */
    private String testAudioPath;

    /**
     * 测试分数
     */
    private Float Score;

    private Date testTime;

    /**
     * 测试音频的地址
     */
    private String audioPath;

    private int index;

    private String userText;

    private String testText;

    /**
     * 错误位置
     */
    private String errorPositions;

    /**
     * 错误标签
     */
    private String errorTags;

    /**
     * 结果分析
     */
    private String resultAnalysis;

    /**
     * 用户语音时长（秒）
     */
    private Float speechDurationSec;

    /**
     * 标准音频时长（秒）
     */
    private Float standardDurationSec;

    /**
     * 时长得分
     */
    private Float durationScore;

    /**
     * 编辑距离得分
     */
    private Float editDistanceScore;

    /**
     * AI评分
     */
    private Float aiScore;

}
