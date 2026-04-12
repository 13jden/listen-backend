package com.example.wx.service.impl;

import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.utils.StringTools;
import com.example.wx.mapper.UsertestMapper;
import com.example.wx.mapper.UserPhaseReportMapper;
import com.example.wx.pojo.UserPhaseReport;
import com.example.wx.pojo.Usertest;
import com.example.wx.service.UserPhaseReportService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 用户阶段性报告服务实现
 */
@Slf4j
@Service
public class UserPhaseReportServiceImpl implements UserPhaseReportService {

    @Autowired
    private UserPhaseReportMapper userPhaseReportMapper;

    @Autowired
    private UsertestMapper usertestMapper;

    @Value("${aliyun.phase-analysis.app-id:}")
    private String analysisAppId;

    @Value("${aliyun.phase-analysis.api-key:}")
    private String analysisApiKey;

    private static final Gson gson = new Gson();

    @Override
    public UserPhaseReport getReportByUserId(String userId) {
        QueryWrapper<UserPhaseReport> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        return userPhaseReportMapper.selectOne(wrapper);
    }

    @Override
    public void generateReport(String userId) {
        // 1. 查询用户所有测试记录
        List<Usertest> testList = usertestMapper.getTestByUserId(userId);
        if (testList == null || testList.isEmpty()) {
            log.warn("用户无测试记录: userId={}", userId);
            return;
        }

        // 2. 统计
        int totalTestCount = testList.size();
        double avgScore = 0;
        List<Double> scoreTrend = new ArrayList<>();

        double sum = 0;
        for (Usertest test : testList) {
            double score = test.getAvgScore();
            if (score != 0) {
                sum += score;
                scoreTrend.add(score);
            }
        }
        avgScore = totalTestCount > 0 ? sum / totalTestCount : 0;

        // 得分趋势数组（按时间正序）
        Collections.reverse(scoreTrend);
        String scoreTrendJson = gson.toJson(scoreTrend);

        // 3. 调用AI生成分析
        String testAnalysis = "";
        try {
            testAnalysis = callAIForAnalysis(userId, testList);
        } catch (Exception e) {
            log.warn("AI分析生成失败: {}", e.getMessage());
        }

        // 4. 保存/更新报告
        UserPhaseReport existReport = userPhaseReportMapper.selectByUserId(userId);
        if (existReport != null) {
            existReport.setTotalTestCount(totalTestCount);
            existReport.setAvgScore(BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP));
            existReport.setScoreTrend(scoreTrendJson);
            existReport.setTestAnalysis(testAnalysis);
            existReport.setReportDate(new Date());
            existReport.setUpdatedAt(new Date());
            userPhaseReportMapper.updateById(existReport);
        } else {
            UserPhaseReport report = new UserPhaseReport();
            report.setId(StringTools.getRandomBumber(32));
            report.setUserId(userId);
            report.setTotalTestCount(totalTestCount);
            report.setAvgScore(BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP));
            report.setScoreTrend(scoreTrendJson);
            report.setTestAnalysis(testAnalysis);
            report.setReportDate(new Date());
            report.setCreatedAt(new Date());
            report.setUpdatedAt(new Date());
            userPhaseReportMapper.insert(report);
        }
        log.info("用户阶段性报告生成成功: userId={}, totalCount={}, avgScore={}", userId, totalTestCount, avgScore);
    }

    @Override
    public void saveDoctorAdvice(String userId, String doctorComment, String doctorSuggestion) {
        UserPhaseReport existReport = userPhaseReportMapper.selectByUserId(userId);
        if (existReport != null) {
            existReport.setDoctorComment(doctorComment);
            existReport.setDoctorSuggestion(doctorSuggestion);
            existReport.setUpdatedAt(new Date());
            userPhaseReportMapper.updateById(existReport);
        } else {
            UserPhaseReport report = new UserPhaseReport();
            report.setId(StringTools.getRandomBumber(32));
            report.setUserId(userId);
            report.setDoctorComment(doctorComment);
            report.setDoctorSuggestion(doctorSuggestion);
            report.setCreatedAt(new Date());
            report.setUpdatedAt(new Date());
            userPhaseReportMapper.insert(report);
        }
        log.info("医生建议保存成功: userId={}", userId);
    }

    /**
     * 调用AI生成分析
     */
    private String callAIForAnalysis(String userId, List<Usertest> testList) {
        try {
            if (analysisAppId == null || analysisAppId.isEmpty() ||
                analysisApiKey == null || analysisApiKey.isEmpty()) {
                log.warn("AI分析配置不完整，跳过AI调用");
                return "";
            }

            // 构建测试历史数据
            List<Map<String, Object>> testHistory = new ArrayList<>();
            for (Usertest test : testList) {
                Map<String, Object> item = new HashMap<>();
                item.put("testTime", test.getEndTime() != null ? test.getEndTime().toString() : "");
                item.put("score", test.getAvgScore() != 0 ? test.getAvgScore() : 0);
                testHistory.add(item);
            }

            // 构建bizParams
            JsonObject bizParams = new JsonObject();
            bizParams.addProperty("data", gson.toJson(testHistory));

            ApplicationParam param = ApplicationParam.builder()
                    .apiKey(analysisApiKey)
                    .appId(analysisAppId)
                    .prompt("请根据用户测试历史数据生成JSON格式的分析")
                    .bizParams(bizParams)
                    .incrementalOutput(false)
                    .hasThoughts(false)
                    .build();

            Application application = new Application();
            ApplicationResult result = application.call(param);

            return extractResult(result);

        } catch (NoApiKeyException | InputRequiredException e) {
            log.error("AI调用异常: {}", e.getMessage());
            return "";
        } catch (Exception e) {
            log.error("AI调用失败: {}", e.getMessage());
            return "";
        }
    }

    private String extractResult(ApplicationResult result) {
        try {
            if (result == null) return "";
            Object output = result.getOutput();
            if (output == null) return "";

            try {
                java.lang.reflect.Method getTextMethod = output.getClass().getMethod("getText");
                Object textObj = getTextMethod.invoke(output);
                if (textObj != null && !textObj.toString().isEmpty()) {
                    return textObj.toString();
                }
            } catch (NoSuchMethodException e) {
                // ignore
            }

            String str = output.toString();
            return (str != null && !str.isEmpty()) ? str : "";
        } catch (Exception e) {
            log.error("提取AI结果失败: {}", e.getMessage());
            return "";
        }
    }
}
