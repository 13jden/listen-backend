package com.example.common.dto;

import java.util.Date;

public class IndexDataVO {
    private int newUserNum;          // 今日新增用户数
    private int userImprove;         // 用户数变化百分比
    private int userNum;             // 用户总数
    private int userNumImprove;      // 用户总数变化百分比
    private int newTestTimes;        // 今日测试次数
    private int newTimesImprove;     // 测试次数变化百分比
    private int allTestTimes;        // 总测试次数
    private int allTimesImprove;     // 总测试次数变化百分比
    private Date time;

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    // Getters and Setters
    public int getNewUserNum() {
        return newUserNum;
    }

    public void setNewUserNum(int newUserNum) {
        this.newUserNum = newUserNum;
    }

    public int getUserImprove() {
        return userImprove;
    }

    public void setUserImprove(int userImprove) {
        this.userImprove = userImprove;
    }

    public int getUserNum() {
        return userNum;
    }

    public void setUserNum(int userNum) {
        this.userNum = userNum;
    }

    public int getUserNumImprove() {
        return userNumImprove;
    }

    public void setUserNumImprove(int userNumImprove) {
        this.userNumImprove = userNumImprove;
    }

    public int getNewTestTimes() {
        return newTestTimes;
    }

    public void setNewTestTimes(int newTestTimes) {
        this.newTestTimes = newTestTimes;
    }

    public int getNewTimesImprove() {
        return newTimesImprove;
    }

    public void setNewTimesImprove(int newTimesImprove) {
        this.newTimesImprove = newTimesImprove;
    }

    public int getAllTestTimes() {
        return allTestTimes;
    }

    public void setAllTestTimes(int allTestTimes) {
        this.allTestTimes = allTestTimes;
    }

    public int getAllTimesImprove() {
        return allTimesImprove;
    }

    public void setAllTimesImprove(int allTimesImprove) {
        this.allTimesImprove = allTimesImprove;
    }
}