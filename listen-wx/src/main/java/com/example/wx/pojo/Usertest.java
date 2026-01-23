package com.example.wx.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
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
public class Usertest implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 测试id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private String id;

    private String userId;

    private String testFilePath;

    /**
     * 测试平均分
     */
    private double avgScore;

    private Date testTime;

    private Date endTime;

    /**
     * 完成的测试个数
     */
    private Integer num;


}
