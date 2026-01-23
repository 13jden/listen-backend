package com.example.wx.mapper;

import com.example.wx.pojo.Hospital;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author dzk
 * @since 2025-01-24
 */
@Mapper
public interface HospitalMapper extends BaseMapper<Hospital> {
    @Select("SELECT * FROM hospital")
    List<Hospital> getAll();
}
