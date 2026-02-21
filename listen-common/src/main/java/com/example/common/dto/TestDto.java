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
    private int Score;

    private Date testTime;

    /**
     * 测试音频的地址
     */
    private String audioPath;

    private int index;

    private String userText;

    private String testText;

}
