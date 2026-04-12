package com.example.wx.service;

import com.example.wx.pojo.UserTestReport;

/**
 * 用户测试报告服务接口
 */
public interface UserTestReportService {

    /**
     * 根据testId查询报告
     */
    UserTestReport getReportByTestId(String testId);

    /**
     * 生成并保存报告
     * @param testId 测试ID
     */
    void generateAndSaveReport(String testId);
}
