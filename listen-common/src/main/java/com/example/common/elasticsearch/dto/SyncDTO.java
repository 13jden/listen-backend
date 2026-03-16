package com.example.common.elasticsearch.dto;

import lombok.Data;

import java.util.Date;

/**
 * ES同步DTO
 * 用于手动指定同步范围
 */
@Data
public class SyncDTO {

    /**
     * 是否同步用户
     */
    private Boolean syncUsers;

    /**
     * 是否同步测试
     */
    private Boolean syncTests;

    /**
     * 是否同步测试详情
     */
    private Boolean syncTestItems;

    /**
     * 是否同步音频
     */
    private Boolean syncAudios;

    /**
     * 开始日期
     */
    private Date startDate;

    /**
     * 结束日期
     */
    private Date endDate;
}
