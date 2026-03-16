package com.example.common.elasticsearch.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.DateFormat;

import java.time.LocalDateTime;

/**
 * ES测试汇总索引 - 对应 tests 索引
 * 用于统计主力，支持多维度聚合分析
 */
@Data
@Document(indexName = "tests")
public class TestEs {

    @Id
    @Field(type = FieldType.Keyword)
    private String testId;

    @Field(type = FieldType.Keyword)
    private String userId;

    @Field(type = FieldType.Keyword)
    private String medicalId;

    @Field(type = FieldType.Keyword)
    private String hospitalId;

    @Field(type = FieldType.Keyword)
    private String hospitalName;

    @Field(type = FieldType.Keyword)
    private String ageGroup;

    @Field(type = FieldType.Integer)
    private Integer age;

    @Field(type = FieldType.Keyword)
    private String sex;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime startTime;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime endTime;

    @Field(type = FieldType.Integer)
    private Integer durationSec;

    @Field(type = FieldType.Keyword)
    private String testDate;

    @Field(type = FieldType.Keyword)
    private String testMonth;

    @Field(type = FieldType.Keyword)
    private String completionStatus;

    @Field(type = FieldType.Float)
    private Double totalScore;

    @Field(type = FieldType.Boolean)
    private Boolean passFlag;

    @Field(type = FieldType.Integer)
    private Integer itemCount;

    @Field(type = FieldType.Integer)
    private Integer totalItems;

    @Field(type = FieldType.Integer)
    private Integer networkLatencyMs;

    @Field(type = FieldType.Keyword)
    private String device;

    @Field(type = FieldType.Keyword)
    private String osVersion;

    @Field(type = FieldType.Keyword)
    private String appVersion;

    @Field(type = FieldType.Keyword)
    private String asrEngine;

    @Field(type = FieldType.Keyword)
    private String modelVersion;
}
