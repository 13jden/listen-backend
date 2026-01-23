package com.example.wx.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
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
 * @since 2025-01-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class Admin implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id号
     */
    @TableId(value = "admin_id", type = IdType.AUTO)
    private String adminId;

    /**
     * 管理员名
     */
    private String name;

    private String password;

    /**
     * 管理员等级0/1（能否添加新管理员）
     */
    private Boolean adminLevel;


}
