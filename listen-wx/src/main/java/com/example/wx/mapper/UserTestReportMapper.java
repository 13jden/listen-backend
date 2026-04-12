package com.example.wx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wx.pojo.UserTestReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户测试报告Mapper
 */
@Mapper
public interface UserTestReportMapper extends BaseMapper<UserTestReport> {

    /**
     * 根据testId查询报告
     */
    @Select("SELECT * FROM user_test_report WHERE test_id = #{testId}")
    UserTestReport selectByTestId(String testId);
}
