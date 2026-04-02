package com.example.wx.elasticsearch.service;

import com.alibaba.dashscope.app.Application;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI评分服务
 * 使用通义千问App对用户发音进行评分
 */
@Slf4j
@Service
public class AIEvaluationService {

    @Value("${aliyun.test.app-id:}")
    private String appId;

    @Value("${aliyun.test.api-key:}")
    private String apiKey;

    @Value("${aliyun.test.work.space.id:}")
    private String workspaceId;

    @Autowired
    private Gson gson;

    /**
     * AI评分结果
     */
    public static class AIEvaluationResult {
        /**
         * AI评分（0-100）
         */
        private float score;

        /**
         * 错误标签（如"平翘舌","前后鼻音"等，用逗号分隔）
         */
        private String errorTags;

        /**
         * 错误详情/结果分析
         */
        private String resultAnalysis;

        /**
         * 错误详情（如"带->打"，表示用户把"带"读成了"打"）
         */
        private String errorDetail;

        public float getScore() {
            return score;
        }

        public void setScore(float score) {
            this.score = score;
        }

        public String getErrorTags() {
            return errorTags;
        }

        public void setErrorTags(String errorTags) {
            this.errorTags = errorTags;
        }

        public String getResultAnalysis() {
            return resultAnalysis;
        }

        public void setResultAnalysis(String resultAnalysis) {
            this.resultAnalysis = resultAnalysis;
        }

        public String getErrorDetail() {
            return errorDetail;
        }

        public void setErrorDetail(String errorDetail) {
            this.errorDetail = errorDetail;
        }
    }

    /**
     * 评估用户发音
     * 如果编辑距离相似度达到100%，则不调用AI，直接返回满分100
     *
     * @param userContent   用户ASR识别文本
     * @param audioContent  标准音频文本
     * @return AI评估结果
     */
    public AIEvaluationResult evaluate(String userContent, String audioContent) {
        // 计算编辑距离相似度
        float similarity = calculateTextSimilarity(userContent, audioContent);

        // 如果相似度达到100%，直接返回满分，不需要AI评分
        if (similarity >= 100f) {
            log.info("文本相似度达到100%，直接返回满分，无需AI评分");
            AIEvaluationResult result = new AIEvaluationResult();
            result.setScore(100f);
            result.setErrorTags("");
            result.setResultAnalysis("您的发音非常标准，与标准文本完全一致！");
            return result;
        }

        // 调用AI进行评分
        return evaluateByAI(userContent, audioContent);
    }

    /**
     * 计算文本相似度（基于编辑距离）
     */
    private float calculateTextSimilarity(String userContent, String audioContent) {
        if (userContent == null || audioContent == null) {
            return 0;
        }

        // 去除非中文字符
        String userText = userContent.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");
        String audioText = audioContent.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");

        // 计算编辑距离
        int distance = levenshteinDistance(userText, audioText);

        // 计算相似度百分比
        int maxLength = Math.max(userText.length(), audioText.length());
        if (maxLength == 0) {
            return 100f;
        }

        return (float) ((1 - (double) distance / maxLength) * 100);
    }

    /**
     * 调用通义千问App进行AI评分
     */
    private AIEvaluationResult evaluateByAI(String userContent, String audioContent) {
        try {
            // 构建业务参数
            Map<String, Object> bizParams = new HashMap<>();
            bizParams.put("userText", userContent);
            bizParams.put("standardText", audioContent);

            JsonObject bizParamJson = buildBizParams(bizParams);

            log.info("AI评分请求参数 - userText: {}, standardText: {}", userContent, audioContent);

            ApplicationParam param = ApplicationParam.builder()
                    .apiKey(apiKey)
                    .appId(appId)
                    .prompt("请根据用户发音和标准文本进行评分")
                    .bizParams(bizParamJson)
                    .incrementalOutput(false)
                    .hasThoughts(false)
                    .build();

            Application application = new Application();
            ApplicationResult resultFlowable = application.call(param);

            String aiResponse = extractResult(resultFlowable);
            log.info("AI评分返回结果: {}", aiResponse);

            return parseAIResponse(aiResponse);

        } catch (NoApiKeyException | InputRequiredException e) {
            log.error("AI调用异常: {}", e.getMessage(), e);
            // 失败时返回编辑距离得分作为兜底
            return createFallbackResult(userContent, audioContent);
        } catch (Exception e) {
            log.error("AI评分失败: {}", e.getMessage(), e);
            return createFallbackResult(userContent, audioContent);
        }
    }

    /**
     * 构建业务参数
     */
    private JsonObject buildBizParams(Map<String, ?> rawParams) {
        JsonObject bizParams = new JsonObject();
        if (rawParams == null || rawParams.isEmpty()) {
            return bizParams;
        }
        rawParams.forEach((key, value) -> {
            if (key == null) {
                return;
            }
            if (value == null) {
                bizParams.addProperty(key, "");
            } else if (value instanceof Number) {
                bizParams.addProperty(key, (Number) value);
            } else if (value instanceof Boolean) {
                bizParams.addProperty(key, (Boolean) value);
            } else if (value instanceof Character) {
                bizParams.addProperty(key, value.toString());
            } else if (value instanceof String) {
                bizParams.addProperty(key, (String) value);
            } else {
                bizParams.addProperty(key, value.toString());
            }
        });
        return bizParams;
    }

    /**
     * 从AI结果中提取评分
     */
    private String extractResult(ApplicationResult result) {
        try {
            if (result == null) {
                log.warn("AI返回结果为空");
                return null;
            }
            
            // 尝试从ApplicationOutput获取文本
            Object output = result.getOutput();
            if (output == null) {
                log.warn("AI返回的output为空");
                return null;
            }
            
            // 尝试getText方法
            try {
                java.lang.reflect.Method getTextMethod = output.getClass().getMethod("getText");
                Object textObj = getTextMethod.invoke(output);
                if (textObj != null && !textObj.toString().isEmpty()) {
                    log.info("AI返回文本: {}", textObj);
                    return textObj.toString();
                }
            } catch (NoSuchMethodException e) {
                // getText不存在
            }
            
            // 尝试toString
            String str = output.toString();
            if (str != null && !str.isEmpty()) {
                log.info("AI返回文本(toString): {}", str);
                return str;
            }
            
            log.warn("AI返回的text为空");
            return null;
        } catch (Exception e) {
            log.error("提取AI结果失败", e);
            return null;
        }
    }

    /**
     * 解析AI响应，提取评分、错误标签和结果分析
     */
    private AIEvaluationResult parseAIResponse(String aiResponse) {
        AIEvaluationResult result = new AIEvaluationResult();

        if (aiResponse == null || aiResponse.isEmpty()) {
            return createEmptyResult();
        }

        // 尝试解析JSON格式的响应
        try {
            JsonObject jsonResponse = gson.fromJson(aiResponse, JsonObject.class);

            // 提取评分（score 或 评分）- 如果没有则使用编辑距离计算
            if (jsonResponse.has("score")) {
                result.setScore(jsonResponse.get("score").getAsFloat());
            } else if (jsonResponse.has("评分")) {
                result.setScore(Float.parseFloat(jsonResponse.get("评分").getAsString()));
            } else {
                // 没有评分字段，后续会用编辑距离算法计算
            }

            // 提取错误详情 -> 错误位置
            if (jsonResponse.has("error")) {
                result.setErrorDetail(jsonResponse.get("error").getAsString());
            }

            // 提取错误标签（errorTags 或 errortag 或 错误标签）
            if (jsonResponse.has("errorTags")) {
                result.setErrorTags(jsonResponse.get("errorTags").getAsString());
            } else if (jsonResponse.has("errortag")) {
                result.setErrorTags(jsonResponse.get("errortag").getAsString());
            } else if (jsonResponse.has("错误标签")) {
                result.setErrorTags(jsonResponse.get("错误标签").getAsString());
            }

            // 提取结果分析（resultAnalysis 或 suggestion 或 结果分析）
            if (jsonResponse.has("resultAnalysis")) {
                result.setResultAnalysis(jsonResponse.get("resultAnalysis").getAsString());
            } else if (jsonResponse.has("suggestion")) {
                result.setResultAnalysis(jsonResponse.get("suggestion").getAsString());
            } else if (jsonResponse.has("结果分析")) {
                result.setResultAnalysis(jsonResponse.get("结果分析").getAsString());
            } else {
                // 如果没有明确的字段，整个JSON作为结果分析
                result.setResultAnalysis(aiResponse);
            }

        } catch (Exception e) {
            // 如果不是JSON格式，尝试从文本中提取评分
            log.warn("AI响应非JSON格式，尝试文本解析: {}", aiResponse);
            parseTextResponse(aiResponse, result);
        }

        // 确保评分有效（如果没有从AI获取到评分，后续会在TestdetailServiceImpl中用编辑距离计算）
        if (result.getScore() > 100) {
            result.setScore(100);
        }

        return result;
    }

    /**
     * 从文本中解析评分结果
     */
    private void parseTextResponse(String text, AIEvaluationResult result) {
        // 提取评分数字
        Pattern scorePattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*分");
        Matcher scoreMatcher = scorePattern.matcher(text);
        if (scoreMatcher.find()) {
            result.setScore(Float.parseFloat(scoreMatcher.group(1)));
        }

        // 提取错误标签
        StringBuilder errorTags = new StringBuilder();
        String[] possibleTags = {"平翘舌", "前后鼻音", "nl混淆", "hf混淆", "送气音", "发音错误"};
        for (String tag : possibleTags) {
            if (text.contains(tag)) {
                if (errorTags.length() > 0) {
                    errorTags.append(",");
                }
                errorTags.append(tag);
            }
        }
        result.setErrorTags(errorTags.toString());
        result.setResultAnalysis(text);
    }

    /**
     * 创建空结果
     */
    private AIEvaluationResult createEmptyResult() {
        AIEvaluationResult result = new AIEvaluationResult();
        result.setScore(0);
        result.setErrorTags("");
        result.setResultAnalysis("AI评分失败");
        return result;
    }

    /**
     * 创建兜底结果（使用编辑距离评分）
     */
    private AIEvaluationResult createFallbackResult(String userContent, String audioContent) {
        AIEvaluationResult result = new AIEvaluationResult();
        float similarity = calculateTextSimilarity(userContent, audioContent);
        result.setScore(similarity);

        // 简单的错误标签分析
        StringBuilder errorTags = new StringBuilder();
        String userText = userContent.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");
        String audioText = audioContent.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");

        // 检测平翘舌
        if (containsPinyin(userText, "zh,ch,sh") && containsPinyin(audioText, "z,c,s") ||
            containsPinyin(audioText, "zh,ch,sh") && containsPinyin(userText, "z,c,s")) {
            errorTags.append("平翘舌");
        }
        // 检测前后鼻音
        if (containsPinyin(userText, "ing,eng") && containsPinyin(audioText, "in,en") ||
            containsPinyin(audioText, "ing,eng") && containsPinyin(userText, "in,en")) {
            if (errorTags.length() > 0) errorTags.append(",");
            errorTags.append("前后鼻音");
        }

        result.setErrorTags(errorTags.toString());
        result.setResultAnalysis("AI评分服务不可用，已使用本地算法评分");

        return result;
    }

    /**
     * 检查是否包含指定拼音
     */
    private boolean containsPinyin(String text, String pinyinList) {
        String[] pinyins = pinyinList.split(",");
        for (String py : pinyins) {
            if (text.contains(py)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算Levenshtein编辑距离
     */
    private int levenshteinDistance(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0;
        }

        int m = s1.length();
        int n = s2.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(
                            dp[i - 1][j] + 1,
                            Math.min(dp[i][j - 1] + 1, dp[i - 1][j - 1] + 1)
                    );
                }
            }
        }

        return dp[m][n];
    }
}
