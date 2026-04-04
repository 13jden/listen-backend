package com.example.wx.elasticsearch.service;

import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationOutput;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * AI辅助分析服务
 * 基于ES聚合数据，调用AI进行综合分析
 */
@Slf4j
@Service
public class AIAnalysisService {

    @Value("${aliyun.analysis.app-id:}")
    private String analysisAppId;

    @Value("${aliyun.analysis.api-key:}")
    private String analysisApiKey;

    @Value("${aliyun.analysis.workspace-id:}")
    private String analysisWorkspaceId;

    @Value("${aliyun.test.app-id:}")
    private String testAppId;

    @Value("${aliyun.test.api-key:}")
    private String testApiKey;

    @Value("${aliyun.test.work.space.id:}")
    private String testWorkspaceId;

    @Autowired
    private ElasticsearchAggregationService aggregationService;

    @Autowired
    private Gson gson;

    /**
     * AI分析请求参数
     */
    @lombok.Data
    public static class AnalysisRequest {
        private String ageGroup;           // 年龄段（可多选，逗号分隔）
        private String hospitalId;          // 医院ID（可多选）
        private String startDate;          // 开始日期
        private String endDate;            // 结束日期
        private String userIds;            // 用户ID（可多选）
        private String content;             // 自定义分析方向（如：侧重平翘舌区分）
        private boolean includeTrend;      // 是否包含趋势分析
        private boolean includeSuggestion; // 是否包含智能建议
        private boolean includeErrorAnalysis; // 是否包含错误分析
        private boolean includeScorePrediction; // 是否包含得分预测
    }

    /**
     * AI分析结果
     */
    @lombok.Data
    public static class AnalysisResult {
        private String summary;            // 分析摘要
        private String focusAnalysis;      // 侧重分析
        private String prediction;         // 预测分析
        private String suggestion;         // 智能建议
        private String errorAnalysis;      // 错误分析
        private String scoreTrend;         // 得分趋势
    }

    /**
     * 执行AI辅助分析
     */
    public AnalysisResult analyze(AnalysisRequest request) {
        log.info("开始AI辅助分析，请求参数: {}", gson.toJson(request));

        try {
            // 1. 获取聚合数据
            Map<String, Object> aggregationData = collectAggregationData(request);

            // 2. 构建AI提示词
            String prompt = buildAnalysisPrompt(request, aggregationData);

            // 3. 调用AI进行分析
            String aiResponse = callAI(prompt);

            // 4. 解析AI响应
            return parseAIResponse(aiResponse);

        } catch (Exception e) {
            log.error("AI辅助分析失败", e);
            AnalysisResult fallback = new AnalysisResult();
            fallback.setSummary("分析失败，请稍后重试");
            return fallback;
        }
    }

    /**
     * 收集聚合数据
     */
    private Map<String, Object> collectAggregationData(AnalysisRequest request) {
        Map<String, Object> data = new HashMap<>();

        try {
            // 年龄分布
            var ageDist = aggregationService.getAgeGroupDistribution(request.getStartDate(), request.getEndDate());
            data.put("ageDistribution", ageDist);

            // 月度趋势
            var monthlyTrend = aggregationService.getMonthlyTrend(request.getStartDate(), request.getEndDate());
            data.put("monthlyTrend", monthlyTrend);

            // 完成状态
            var completionStatus = aggregationService.getCompletionStatus(request.getStartDate(), request.getEndDate());
            data.put("completionStatus", completionStatus);

            // 错误类型
            var errorTypes = aggregationService.getErrorTypeDistribution(request.getStartDate(), request.getEndDate());
            data.put("errorTypes", errorTypes);

            // 医院统计
            var hospitalStats = aggregationService.getHospitalStats(request.getStartDate(), request.getEndDate());
            data.put("hospitalStats", hospitalStats);

            // 得分分布
            var scoreDist = aggregationService.getScoreDistribution(request.getStartDate(), request.getEndDate());
            data.put("scoreDistribution", scoreDist);

            // 统计总计
            var summaryStats = aggregationService.getSummaryStats();
            data.put("summaryStats", summaryStats);

        } catch (Exception e) {
            log.error("收集聚合数据失败", e);
        }

        return data;
    }

    /**
     * 构建分析提示词
     */
    private String buildAnalysisPrompt(AnalysisRequest request, Map<String, Object> aggregationData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位专业的听力言语康复分析专家，请根据以下数据进行分析：\n\n");

        // 基本统计
        prompt.append("【基本统计】\n");
        var summaryStats = (com.example.wx.elasticsearch.vo.SummaryStatsVO) aggregationData.get("summaryStats");
        if (summaryStats != null) {
            prompt.append(String.format("- 总用户数: %d\n", summaryStats.getTotalUsers()));
            prompt.append(String.format("- 总测试数: %d\n", summaryStats.getTotalTests()));
            prompt.append(String.format("- 平均得分: %.1f分\n", summaryStats.getAvgScore()));
        }

        // 年龄分布
        prompt.append("\n【年龄段分布】\n");
        var ageDist = (com.example.wx.elasticsearch.vo.AgeGroupDistributionVO) aggregationData.get("ageDistribution");
        if (ageDist != null && ageDist.getData() != null) {
            for (var item : ageDist.getData()) {
                prompt.append(String.format("- %s: %d人, 平均分 %.1f\n",
                        item.getAgeGroup(), item.getCount(), item.getAvgScore()));
            }
        }

        // 错误类型
        prompt.append("\n【常见错误类型 TOP10】\n");
        var errorTypes = (com.example.wx.elasticsearch.vo.ErrorTypeDistributionVO) aggregationData.get("errorTypes");
        if (errorTypes != null && errorTypes.getData() != null) {
            int count = 0;
            for (var item : errorTypes.getData()) {
                if (count++ >= 10) break;
                prompt.append(String.format("- %s: %d次\n", item.getErrorType(), item.getCount()));
            }
        }

        // 月度趋势
        prompt.append("\n【月度测试趋势】\n");
        var monthlyTrend = (com.example.wx.elasticsearch.vo.MonthlyTrendVO) aggregationData.get("monthlyTrend");
        if (monthlyTrend != null) {
            if (monthlyTrend.getMonths() != null) {
                for (int i = 0; i < monthlyTrend.getMonths().size(); i++) {
                    String month = monthlyTrend.getMonths().get(i);
                    Long count = i < monthlyTrend.getTestCounts().size() ? monthlyTrend.getTestCounts().get(i) : 0L;
                    Double avg = i < monthlyTrend.getAvgScores().size() ? monthlyTrend.getAvgScores().get(i) : 0.0;
                    prompt.append(String.format("- %s: %d次, 平均分 %.1f\n", month, count, avg));
                }
            }
        }

        // 得分分布
        prompt.append("\n【得分分布】\n");
        var scoreDist = (com.example.wx.elasticsearch.vo.ScoreDistributionVO) aggregationData.get("scoreDistribution");
        if (scoreDist != null && scoreDist.getRanges() != null) {
            for (int i = 0; i < scoreDist.getRanges().size(); i++) {
                String range = scoreDist.getRanges().get(i);
                Long count = i < scoreDist.getCounts().size() ? scoreDist.getCounts().get(i) : 0L;
                prompt.append(String.format("- %s分: %d人\n", range, count));
            }
        }

        // 过滤条件
        prompt.append("\n【分析条件】\n");
        if (request.getAgeGroup() != null && !request.getAgeGroup().isEmpty()) {
            prompt.append(String.format("- 关注年龄段: %s\n", request.getAgeGroup()));
        }
        if (request.getHospitalId() != null && !request.getHospitalId().isEmpty()) {
            prompt.append(String.format("- 关注医院: %s\n", request.getHospitalId()));
        }
        if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
            prompt.append(String.format("- 时间范围: %s 至 %s\n", request.getStartDate(), request.getEndDate()));
        }
        if (request.getContent() != null && !request.getContent().isEmpty()) {
            prompt.append(String.format("- 重点分析方向: %s\n", request.getContent()));
        }

        // 分析要求
        prompt.append("\n【分析要求】\n");
        prompt.append("请返回JSON格式的分析结果，包含以下字段：\n");
        prompt.append("{\n");
        prompt.append("  \"summary\": \"整体分析摘要\",\n");
        if (request.isIncludeTrend()) {
            prompt.append("  \"scoreTrend\": \"得分趋势分析\",\n");
        }
        if (request.isIncludeErrorAnalysis()) {
            prompt.append("  \"errorAnalysis\": \"错误原因详细分析\",\n");
        }
        if (request.isIncludeSuggestion()) {
            prompt.append("  \"suggestion\": \"针对性训练建议\",\n");
        }
        if (request.isIncludeScorePrediction()) {
            prompt.append("  \"prediction\": \"得分预测与目标设定\"\n");
        }
        prompt.append("}\n");

        return prompt.toString();
    }

    /**
     * 调用AI接口
     */
    private String callAI(String prompt) {
        try {
            // 使用分析专用应用
            String appId = !analysisAppId.isEmpty() ? analysisAppId : testAppId;
            String apiKeyValue = !analysisApiKey.isEmpty() ? analysisApiKey : testApiKey;

            if (appId.isEmpty() || apiKeyValue.isEmpty()) {
                log.warn("AI分析配置不完整，跳过AI调用");
                return "{}";
            }

            JsonObject bizParams = new JsonObject();
            bizParams.addProperty("data", prompt);

            ApplicationParam param = ApplicationParam.builder()
                    .apiKey(apiKeyValue)
                    .appId(appId)
                    .prompt("请分析以下数据并返回JSON格式的建议")
                    .bizParams(bizParams)
                    .incrementalOutput(false)
                    .hasThoughts(false)
                    .build();

            Application application = new Application();
            ApplicationResult result = application.call(param);

            return extractResult(result);

        } catch (NoApiKeyException | InputRequiredException e) {
            log.error("AI调用异常: {}", e.getMessage(), e);
            return "{}";
        } catch (Exception e) {
            log.error("AI调用失败", e);
            return "{}";
        }
    }

    private String extractResult(ApplicationResult result) {
        try {
            if (result == null) return "{}";
            
            // 尝试从ApplicationOutput获取文本
            Object output = result.getOutput();
            if (output == null) return "{}";
            
            // 尝试getText方法
            try {
                java.lang.reflect.Method getTextMethod = output.getClass().getMethod("getText");
                Object textObj = getTextMethod.invoke(output);
                if (textObj != null && !textObj.toString().isEmpty()) {
                    return textObj.toString();
                }
            } catch (NoSuchMethodException e) {
                // getText不存在，尝试其他方式
            }
            
            // 尝试toString
            String str = output.toString();
            if (str != null && !str.isEmpty()) {
                return str;
            }
            
            return "{}";
        } catch (Exception e) {
            log.error("提取AI结果失败", e);
            return "{}";
        }
    }

    /**
     * 解析AI响应
     */
    private AnalysisResult parseAIResponse(String aiResponse) {
        AnalysisResult result = new AnalysisResult();

        try {
            // 尝试解析JSON
            com.google.gson.JsonObject json = new com.google.gson.JsonParser().parse(aiResponse).getAsJsonObject();

            if (json.has("summary")) {
                result.setSummary(json.get("summary").getAsString());
            }
            if (json.has("focusAnalysis")) {
                result.setFocusAnalysis(json.get("focusAnalysis").getAsString());
            }
            if (json.has("prediction")) {
                result.setPrediction(json.get("prediction").getAsString());
            }
            if (json.has("suggestion")) {
                result.setSuggestion(json.get("suggestion").getAsString());
            }
            if (json.has("errorAnalysis")) {
                result.setErrorAnalysis(json.get("errorAnalysis").getAsString());
            }
            if (json.has("scoreTrend")) {
                result.setScoreTrend(json.get("scoreTrend").getAsString());
            }

        } catch (Exception e) {
            log.warn("AI响应JSON解析失败，使用原始文本: {}", aiResponse);
            // 解析失败时，直接返回原始文本
            result.setSummary(aiResponse);
        }

        return result;
    }

    /**
     * 生成月度报告
     */
    public String generateMonthlyReport(String month) {
        try {
            // 设置月份范围
            String startDate = month + "-01";
            String endDate = month + "-31";

            AnalysisRequest request = new AnalysisRequest();
            request.setStartDate(startDate);
            request.setEndDate(endDate);
            request.setIncludeTrend(true);
            request.setIncludeSuggestion(true);
            request.setIncludeErrorAnalysis(true);
            request.setIncludeScorePrediction(true);

            AnalysisResult result = analyze(request);

            // 生成报告文本
            StringBuilder report = new StringBuilder();
            report.append("# ").append(month).append(" 月度分析报告\n\n");
            report.append("## 一、整体概况\n");
            report.append(result.getSummary()).append("\n\n");
            report.append("## 二、得分趋势\n");
            report.append(result.getScoreTrend()).append("\n\n");
            report.append("## 三、错误分析\n");
            report.append(result.getErrorAnalysis()).append("\n\n");
            report.append("## 四、智能建议\n");
            report.append(result.getSuggestion()).append("\n\n");
            if (result.getPrediction() != null) {
                report.append("## 五、预测与目标\n");
                report.append(result.getPrediction()).append("\n");
            }

            return report.toString();

        } catch (Exception e) {
            log.error("生成月度报告失败", e);
            return "报告生成失败，请稍后重试";
        }
    }

    /**
     * 生成医院情况分析
     */
    public String generateHospitalReport(String hospitalId, String hospitalName, String startDate, String endDate) {
        try {
            AnalysisRequest request = new AnalysisRequest();
            request.setHospitalId(hospitalId);
            request.setStartDate(startDate);
            request.setEndDate(endDate);
            request.setIncludeTrend(true);
            request.setIncludeSuggestion(true);
            request.setIncludeErrorAnalysis(true);

            AnalysisResult result = analyze(request);

            StringBuilder report = new StringBuilder();
            report.append("# ").append(hospitalName).append(" 听力康复评估报告\n");
            report.append("（统计周期：").append(startDate).append(" 至 ").append(endDate).append("）\n\n");
            report.append("## 一、整体表现\n");
            report.append(result.getSummary()).append("\n\n");
            report.append("## 二、主要问题\n");
            report.append(result.getErrorAnalysis()).append("\n\n");
            report.append("## 三、改进建议\n");
            report.append(result.getSuggestion());

            return report.toString();

        } catch (Exception e) {
            log.error("生成医院报告失败", e);
            return "报告生成失败，请稍后重试";
        }
    }

    /**
     * 生成老年群体问题专项分析
     */
    public String generateElderlyAnalysis(String startDate, String endDate) {
        try {
            AnalysisRequest request = new AnalysisRequest();
            request.setAgeGroup("60-65,65-70,>=70");
            request.setStartDate(startDate);
            request.setEndDate(endDate);
            request.setIncludeTrend(true);
            request.setIncludeSuggestion(true);
            request.setIncludeErrorAnalysis(true);
            request.setIncludeScorePrediction(true);

            AnalysisResult result = analyze(request);

            StringBuilder report = new StringBuilder();
            report.append("# 老年群体听力康复专项分析\n");
            report.append("（统计周期：").append(startDate).append(" 至 ").append(endDate).append("）\n\n");
            report.append("## 一、群体特征分析\n");
            report.append(result.getSummary()).append("\n\n");
            report.append("## 二、常见问题分析\n");
            report.append(result.getErrorAnalysis()).append("\n\n");
            report.append("## 三、康复建议\n");
            report.append(result.getSuggestion()).append("\n\n");
            report.append("## 四、预期效果\n");
            report.append(result.getPrediction());

            return report.toString();

        } catch (Exception e) {
            log.error("生成老年群体分析失败", e);
            return "分析生成失败，请稍后重试";
        }
    }
}
