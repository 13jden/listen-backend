package com.example.common.dto;

import lombok.Data;

import java.util.Date;

@Data
public class UserDetailInfo {
    private String audioPath;
    private String userAudioPath;
    private String testDetailId;
    private String audioContent;
    private String userContent;
    private int index;
    private Float score;
    private Date time;

}
