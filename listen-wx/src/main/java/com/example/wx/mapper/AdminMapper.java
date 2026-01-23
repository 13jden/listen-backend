package com.example.wx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wx.pojo.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author dzk
 * @since 2025-01-24
 */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
    @Select("SELECT * FROM admin WHERE name = #{name} ")
    Admin selectByName(String name);
}
