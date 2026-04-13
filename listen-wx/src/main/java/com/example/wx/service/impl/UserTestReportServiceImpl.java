package com.example.wx.service.impl;

import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationOutput;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.redis.RedisComponent;
import com.example.common.utils.StringTools;
import com.example.wx.mapper.AudioMapper;
import com.example.wx.mapper.TestdetailMapper;
import com.example.wx.mapper.UsertestMapper;
import com.example.wx.pojo.Audio;
import com.example.wx.pojo.Testdetail;
import com.example.wx.pojo.UserTestReport;
import com.example.wx.pojo.Usertest;
import com.example.wx.mapper.UserTestReportMapper;
import com.example.wx.service.UserTestReportService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户测试报告服务实现
 */
@Slf4j
@Service
public class UserTestReportServiceImpl implements UserTestReportService {

    @Autowired
    private UserTestReportMapper userTestReportMapper;

    @Autowired
    private TestdetailMapper testdetailMapper;

    @Autowired
    private UsertestMapper usertestMapper;

    @Autowired
    private AudioMapper audioMapper;

    @Autowired
    private RedisComponent redisComponent;

    @Value("${aliyun.test-report.app-id:}")
    private String analysisAppId;

    @Value("${aliyun.test-report.api-key:}")
    private String analysisApiKey;

    @Value("${aliyun.test-report.workspace-id:}")
    private String analysisWorkspaceId;

    private static final Gson gson = new Gson();

    @Override
    public UserTestReport getReportByTestId(String testId) {
        QueryWrapper<UserTestReport> wrapper = new QueryWrapper<>();
        wrapper.eq("test_id", testId);
        return userTestReportMapper.selectOne(wrapper);
    }

    @Override
    public void generateAndSaveReport(String testId) {
        // 1. 查询测试详情列表
        List<Testdetail> testdetailList = testdetailMapper.selectListByTestId(testId);
        if (testdetailList == null || testdetailList.isEmpty()) {
            log.warn("测试详情为空，跳过报告生成: testId={}", testId);
            return;
        }

        // 2. 收集错误信息
        List<String> errorTagsList = new ArrayList<>();
        List<String> errorDetails = new ArrayList<>();
        int correctCount = 0;
        int errorCount = 0;
        int totalScoreSum = 0;

        for (Testdetail td : testdetailList) {
            if (td.getScore() != null) {
                totalScoreSum += td.getScore();
                if (td.getScore() >= 60) {
                    correctCount++;
                } else {
                    errorCount++;
                    // 收集错误标签
                    if (td.getErrorTags() != null && !td.getErrorTags().isEmpty()) {
                        String[] tags = td.getErrorTags().split(",");
                        for (String tag : tags) {
                            if (!errorTagsList.contains(tag.trim())) {
                                errorTagsList.add(tag.trim());
                            }
                        }
                    }
                    // 收集错误详情
                    Audio audio = audioMapper.selectById(td.getAudioId());
                    if (audio != null) {
                        String detail = String.format("第%d题: 标准「%s」, 您的回答「%s」",
                                td.getIndex(), audio.getContent(), td.getUserContent());
                        errorDetails.add(detail);
                    }
                }
            }
        }

        // 3. 计算统计数据
        int totalItems = testdetailList.size();
        double avgScore = totalItems > 0 ? (double) totalScoreSum / totalItems : 0;
        String errorSummary = buildErrorSummary(errorDetails);

        // 4. 调用AI获取改善建议和分析
        String improvementSuggestion = "";
        String aiAnalysis = "";
        try {
            String aiResponse = callAIForReport(testdetailList);
            if (aiResponse != null && !aiResponse.isEmpty()) {
                JsonObject json = gson.fromJson(aiResponse, JsonObject.class);
                if (json != null) {
                    if (json.has("improvementSuggestion")) {
                        improvementSuggestion = json.get("improvementSuggestion").getAsString();
                    }
                    if (json.has("analysis")) {
                        aiAnalysis = json.get("analysis").getAsString();
                    }
                    if (improvementSuggestion.isEmpty() && aiAnalysis.isEmpty()) {
                        aiAnalysis = aiResponse;
                    }
                } else {
                    aiAnalysis = aiResponse;
                }
            }
        } catch (Exception e) {
            log.warn("AI报告生成失败: {}", e.getMessage());
        }

        // 5. 保存报告
        UserTestReport report = new UserTestReport();
        report.setId(StringTools.getRandomBumber(32));
        report.setTestId(testId);
        report.setAvgScore(BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP));
        report.setTotalItems(totalItems);
        report.setCorrectCount(correctCount);
        report.setErrorCount(errorCount);
        report.setErrorTags(gson.toJson(errorTagsList));
        report.setErrorSummary(errorSummary);
        report.setImprovementSuggestion(improvementSuggestion);
        report.setAiAnalysis(aiAnalysis);
        report.setCreatedAt(new Date());
        report.setUpdatedAt(new Date());

        userTestReportMapper.insert(report);
        log.info("用户测试报告生成成功: testId={}, avgScore={}", testId, avgScore);
    }

    /**
     * 调用AI生成报告
     */
    private String callAIForReport(List<Testdetail> testdetailList) {
        try {
            if (analysisAppId == null || analysisAppId.isEmpty() ||
                analysisApiKey == null || analysisApiKey.isEmpty()) {
                log.warn("AI分析配置不完整，跳过AI调用");
                return "";
            }

            // 构建测试详情数据
            List<Map<String, Object>> testItems = new ArrayList<>();
            for (Testdetail td : testdetailList) {
                Audio audio = audioMapper.selectById(td.getAudioId());
                Map<String, Object> item = new HashMap<>();
                item.put("index", td.getIndex());
                item.put("audioContent", audio != null ? audio.getContent() : "");
                item.put("userContent", td.getUserContent() != null ? td.getUserContent() : "");
                item.put("score", td.getScore() != null ? td.getScore() : 0);
                item.put("errorTags", td.getErrorTags() != null ? Arrays.asList(td.getErrorTags().split(",")) : new ArrayList<>());
                testItems.add(item);
            }

            String testDataJson = gson.toJson(testItems);

            ApplicationParam param = ApplicationParam.builder()
                    .apiKey(analysisApiKey)
                    .appId(analysisAppId)
                    .prompt("请根据以下测试数据生成JSON格式的改善建议和分析报告。数据：" + testDataJson)
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
            if (str != null && !str.isEmpty()) {
                return str;
            }
            return "";
        } catch (Exception e) {
            log.error("提取AI结果失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 构建错误汇总描述
     */
    private String buildErrorSummary(List<String> errorDetails) {
        if (errorDetails.isEmpty()) {
            return "本次测试全部正确，表现优秀！";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("本次测试共").append(errorDetails.size()).append("题出现错误：\n");
        for (int i = 0; i < Math.min(errorDetails.size(), 5); i++) {
            sb.append("- ").append(errorDetails.get(i)).append("\n");
        }
        if (errorDetails.size() > 5) {
            sb.append("...等其他").append(errorDetails.size() - 5).append("题");
        }
        return sb.toString();
    }
}
