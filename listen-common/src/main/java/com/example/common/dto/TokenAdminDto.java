package com.example.common.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TokenAdminDto {
    /**
     * id号
     */
    private String adminId;

    /**
     * 管理员名
     */
    private String name;

    /**
     * 管理员等级0/1（能否添加新管理员）
     */
    private Boolean adminLevel;

    private String token;


}
