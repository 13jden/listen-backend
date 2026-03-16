package com.example.common.elasticsearch.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.DateFormat;

import java.time.LocalDateTime;

/**
 * ES标准语音库索引 - 对应 audios 索引
 * 存储标准音频文件信息
 */
@Data
@Document(indexName = "audios")
public class AudioEs {

    @Id
    @Field(type = FieldType.Keyword)
    private String audioId;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String content;

    @Field(type = FieldType.Keyword)
    private String text;

    @Field(type = FieldType.Keyword)
    private String batch;

    @Field(type = FieldType.Keyword)
    private String version;

    @Field(type = FieldType.Float)
    private Float durationSec;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String difficulty;

    @Field(type = FieldType.Keyword)
    private String uploadAdmin;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime uploadTime;

    @Field(type = FieldType.Keyword)
    private String path;
}
