package com.example.common.dto;

import java.util.Date;

public class UserDetailInfo {
    private String audioPath;
    private String userAudioPath;
    private String testDetailId;
    private String audioContent;
    private String userContent;
    private int index;
    private int score;
    private Date time;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public String getUserAudioPath() {
        return userAudioPath;
    }

    public void setUserAudioPath(String userAudioPath) {
        this.userAudioPath = userAudioPath;
    }

    public String getTestDetailId() {
        return testDetailId;
    }

    public void setTestDetailId(String testDetailId) {
        this.testDetailId = testDetailId;
    }

    public String getAudioContent() {
        return audioContent;
    }

    public void setAudioContent(String audioContent) {
        this.audioContent = audioContent;
    }

    public String getUserContent() {
        return userContent;
    }

    public void setUserContent(String userContent) {
        this.userContent = userContent;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }
}
