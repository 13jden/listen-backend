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
 * @since 2025-01-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "user_id", type = IdType.AUTO)
    private String userId;

    /**
     * 微信授权openid
     */
    private String openId;

    /**
     * 姓名
     */
    private String name;

    /**
     * 手机号
     */
    private String number;

    private Integer hospitalId;

    private String medicalId;

    private Date registerTime;


}
