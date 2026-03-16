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
 * 用于错误分析、样本追溯
 */
@Data
@Document(indexName = "test_items", createIndex = false)
public class TestItemEs {

    @Id
    @Field(type = FieldType.Keyword)
    private String id;

    @Field(type = FieldType.Keyword)
    private String testId;

    @Field(type = FieldType.Integer)
    private Integer itemIndex;

    @Field(type = FieldType.Keyword)
    private String audioId;

    @Field(type = FieldType.Keyword)
    private String audioBatch;

    @Field(type = FieldType.Keyword)
    private String audioVersion;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String promptText;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String standardText;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String userAsrText;

    @Field(type = FieldType.Float)
    private Float speechDurationSec;

    @Field(type = FieldType.Float)
    private Float speechRate;

    @Field(type = FieldType.Integer)
    private Integer editDistance;

    @Field(type = FieldType.Float)
    private Float textSimilarity;

    @Field(type = FieldType.Float)
    private Float llmScore;

    @Field(type = FieldType.Float)
    private Float finalScore;

    @Field(type = FieldType.Boolean)
    private Boolean isCorrect;

    @Field(type = FieldType.Keyword)
    private String[] errorTags;

    @Field(type = FieldType.Text)
    private String errorDetail;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;
}
