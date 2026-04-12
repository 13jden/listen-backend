package com.example.wx.service;

import com.example.wx.pojo.UserPhaseReport;

/**
 * 用户阶段性报告服务接口
 */
public interface UserPhaseReportService {

    /**
     * 根据userId查询报告
     */
    UserPhaseReport getReportByUserId(String userId);

    /**
     * 生成/更新报告（AI分析 + 查询所有测试记录）
     * @param userId 用户ID
     */
    void generateReport(String userId);

    /**
     * 保存医生评论和建议
     * @param userId 用户ID
     * @param doctorComment 医生评语
     * @param doctorSuggestion 医生建议
     */
    void saveDoctorAdvice(String userId, String doctorComment, String doctorSuggestion);
}
