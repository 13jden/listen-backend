package com.example.common.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.util.Date;

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

    public String getUserText() {
        return userText;
    }

    public void setUserText(String userText) {
        this.userText = userText;
    }

    public String getTestText() {
        return testText;
    }

    public void setTestText(String testText) {
        this.testText = testText;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getTest_id() {
        return testId;
    }

    public void setTest_id(String test_id) {
        this.testId = test_id;
    }

    public String getTestAudioPath() {
        return testAudioPath;
    }

    public void setTestAudioPath(String testAudioPath) {
        this.testAudioPath = testAudioPath;
    }

    public int getScore() {
        return Score;
    }

    public void setScore(int score) {
        Score = score;
    }

    public Date getTestTime() {
        return testTime;
    }

    public void setTestTime(Date testTime) {
        this.testTime = testTime;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }
}
