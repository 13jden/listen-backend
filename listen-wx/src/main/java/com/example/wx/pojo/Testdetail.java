package com.example.wx.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author dzk
 * @since 2025-02-06
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class Testdetail implements Serializable {

    private static final long serialVersionUID=1L;

    @TableId(value = "id", type = IdType.AUTO)
    private String id;

    /**
     * 测试id
     */
    private String testId;

    /**
     * 测试音频的地址
     */
    private String audioId;

    private String userAudioPath;

    private Integer index;

    /**
     * 用户得分
     */
    private Float score;

    /**
     * 用户音频（api返回内容）
     */
    private String userContent;

    private Date testTime;

    /**
     * 错误位置（用逗号分隔的位置列表，如 "1,3,5" 表示第1、3、5个字错误）
     */
    private String errorPositions;

    /**
     * 错误标签（用逗号分隔的标签列表，如 "平翘舌,前后鼻音"）
     */
    private String errorTags;

    /**
     * 结果分析（总体评估和建议）
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
     * 时长得分（基于用户时长与标准时长的比例）
     */
    private Float durationScore;

    /**
     * 编辑距离得分（基于Levenshtein距离）
     */
    private Float editDistanceScore;

    /**
     * AI评分（预留）
     */
    private Float aiScore;


}
