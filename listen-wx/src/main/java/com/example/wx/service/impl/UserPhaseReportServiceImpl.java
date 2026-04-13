package com.example.wx.service.impl;

import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.utils.StringTools;
import com.example.wx.mapper.AudioMapper;
import com.example.wx.mapper.TestdetailMapper;
import com.example.wx.mapper.UsertestMapper;
import com.example.wx.mapper.UserPhaseReportMapper;
import com.example.wx.pojo.Audio;
import com.example.wx.pojo.Testdetail;
import com.example.wx.pojo.UserPhaseReport;
import com.example.wx.pojo.Usertest;
import com.example.wx.service.UserPhaseReportService;
import com.google.gson.Gson;
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

    @Autowired
    private TestdetailMapper testdetailMapper;

    @Autowired
    private AudioMapper audioMapper;

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

        // 3. 调用AI生成分析并解析
        String testAnalysis = "";
        try {
            String aiResponse = callAIForAnalysis(userId, testList);
            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                log.warn("AI返回为空，跳过解析");
            } else {
                // 去掉 markdown 代码块包裹
                String jsonStr = aiResponse.trim();
                if (jsonStr.startsWith("```json")) {
                    jsonStr = jsonStr.substring(7);
                } else if (jsonStr.startsWith("```")) {
                    jsonStr = jsonStr.substring(3);
                }
                if (jsonStr.endsWith("```")) {
                    jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
                }
                jsonStr = jsonStr.trim();
                if (jsonStr.isEmpty()) {
                    log.warn("AI返回内容为空字符串");
                } else {
                    try {
                        com.google.gson.JsonObject parsed = gson.fromJson(jsonStr, com.google.gson.JsonObject.class);
                        if (parsed != null) {
                            String evaluation = "";
                            String suggestion = "";

                            // 优先尝试顶层结构：{"evaluation":"...","suggestion":"..."}
                            if (parsed.has("evaluation")) {
                                evaluation = parsed.get("evaluation").getAsString();
                            } else if (parsed.has("analysis")) {
                                // 其次尝试 analysis 字段（可能是字符串，也可能是嵌套对象）
                                com.google.gson.JsonElement analysisElem = parsed.get("analysis");
                                if (analysisElem.isJsonObject()) {
                                    com.google.gson.JsonObject analysisObj = analysisElem.getAsJsonObject();
                                    evaluation = analysisObj.has("evaluation") ? analysisObj.get("evaluation").getAsString() : "";
                                } else {
                                    evaluation = analysisElem.getAsString();
                                }
                            }

                            if (parsed.has("suggestion")) {
                                com.google.gson.JsonElement sugElem = parsed.get("suggestion");
                                if (parsed.has("evaluation") && sugElem.isJsonObject()) {
                                    // suggestion 也在 analysis 里
                                    suggestion = sugElem.getAsJsonObject().has("suggestion")
                                            ? sugElem.getAsJsonObject().get("suggestion").getAsString() : "";
                                } else {
                                    suggestion = sugElem.getAsString();
                                }
                            }

                            StringBuilder sb = new StringBuilder();
                            if (!evaluation.isEmpty()) sb.append("评价：").append(evaluation);
                            if (!suggestion.isEmpty()) {
                                if (sb.length() > 0) sb.append("\n");
                                sb.append("建议：").append(suggestion);
                            }
                            testAnalysis = sb.length() > 0 ? sb.toString() : aiResponse;
                        } else {
                            testAnalysis = aiResponse;
                        }
                    } catch (Exception e) {
                        log.warn("JSON解析失败，保存原始内容: {}", e.getMessage());
                        testAnalysis = aiResponse;
                    }
                }
            }
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

            // 构建完整测试数据（含每段testdetail）
            List<Map<String, Object>> testHistory = new ArrayList<>();
            for (Usertest test : testList) {
                List<Testdetail> details = testdetailMapper.selectListByTestId(test.getId());
                List<Map<String, Object>> detailItems = new ArrayList<>();
                for (Testdetail td : details) {
                    Audio audio = audioMapper.selectById(td.getAudioId());
                    Map<String, Object> d = new HashMap<>();
                    d.put("index", td.getIndex());
                    d.put("standardContent", audio != null ? audio.getContent() : "");
                    d.put("userContent", td.getUserContent() != null ? td.getUserContent() : "");
                    d.put("score", td.getScore() != null ? td.getScore() : 0);
                    d.put("errorTags", td.getErrorTags() != null ? Arrays.asList(td.getErrorTags().split(",")) : new ArrayList<>());
                    d.put("resultAnalysis", td.getResultAnalysis() != null ? td.getResultAnalysis() : "");
                    detailItems.add(d);
                }
                Map<String, Object> item = new HashMap<>();
                item.put("testTime", test.getEndTime() != null ? test.getEndTime().toString() : "");
                item.put("score", test.getAvgScore() != 0 ? test.getAvgScore() : 0);
                item.put("details", detailItems);
                testHistory.add(item);
            }
            String testDataJson = gson.toJson(testHistory);

            ApplicationParam param = ApplicationParam.builder()
                    .apiKey(analysisApiKey)
                    .appId(analysisAppId)
                    .prompt("请根据以下用户测试历史数据，生成一段简洁的分析报告，直接返回JSON格式，结构为{\"evaluation\":\"整体评价文字\",\"suggestion\":\"建议文字\"}，不要用markdown包裹，不要嵌套，不要代码块。数据：" + testDataJson)
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
            if (result == null) {
                log.warn("AI result is null");
                return "";
            }
            Object output = result.getOutput();
            if (output == null) {
                log.warn("AI result output is null");
                return "";
            }

            try {
                java.lang.reflect.Method getTextMethod = output.getClass().getMethod("getText");
                Object textObj = getTextMethod.invoke(output);
                if (textObj != null && !textObj.toString().trim().isEmpty()) {
                    String text = textObj.toString();
                    log.info("AI返回内容: {}", text);
                    return text;
                }
            } catch (NoSuchMethodException e) {
                // ignore
            }

            String str = output.toString();
            if (str != null && !str.trim().isEmpty()) {
                log.info("AI返回内容(toString): {}", str);
                return str;
            }
            log.warn("AI返回内容为空");
            return "";
        } catch (Exception e) {
            log.error("提取AI结果失败: {}", e.getMessage());
            return "";
        }
    }
}
