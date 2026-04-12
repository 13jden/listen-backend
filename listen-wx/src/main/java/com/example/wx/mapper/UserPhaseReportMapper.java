package com.example.wx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wx.pojo.UserPhaseReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户阶段性报告Mapper
 */
@Mapper
public interface UserPhaseReportMapper extends BaseMapper<UserPhaseReport> {

    /**
     * 根据userId查询报告
     */
    @Select("SELECT * FROM user_phase_report WHERE user_id = #{userId}")
    UserPhaseReport selectByUserId(String userId);
}
