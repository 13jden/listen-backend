package com.example.wx.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author dzk
 * @since 2025-02-06
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class Testdetail implements Serializable {

    private static final long serialVersionUID=1L;

    @TableId(value = "id", type = IdType.AUTO)
    private String id;

    /**
     * 测试id
     */
    private String testId;

    /**
     * 测试音频的地址
     */
    private String audioId;

    private String userAudioPath;

    private Integer index;

    /**
     * 用户得分
     */
    private int score;

    /**
     * 用户音频（api返回内容）
     */
    private String userContent;

    private Date testTime;


}
