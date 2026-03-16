package com.example.common.elasticsearch.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.DateFormat;

import java.time.LocalDate;

/**
 * ES用户画像索引 - 对应 users 索引
 * 基于MySQL的User表和用户设计文档创建
 */
@Data
@Document(indexName = "users")
public class UserEs {

    @Id
    @Field(type = FieldType.Keyword)
    private String userId;

    @Field(type = FieldType.Keyword)
    private String openid;

    @Field(type = FieldType.Keyword)
    private String medicalId;

    @Field(type = FieldType.Keyword)
    private String phone;

    @Field(type = FieldType.Keyword)
    private String phoneHash;

    @Field(type = FieldType.Keyword)
    private String hospitalId;

    @Field(type = FieldType.Keyword)
    private String hospitalName;

    @Field(type = FieldType.Keyword)
    private String sex;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate birthDate;

    @Field(type = FieldType.Integer)
    private Integer age;

    @Field(type = FieldType.Keyword)
    private String ageGroup;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate lastTestAt;

    @Field(type = FieldType.Integer)
    private Integer testCount;

    @Field(type = FieldType.Float)
    private Double avgScore;

    @Field(type = FieldType.Float)
    private Double latestScore;

    @Field(type = FieldType.Keyword)
    private String status;
}
