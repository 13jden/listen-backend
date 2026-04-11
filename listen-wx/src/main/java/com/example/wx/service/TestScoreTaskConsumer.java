package com.example.wx.service;

import com.example.common.common.getHttpAudio;
import com.example.common.dto.TestDto;
import com.example.common.dto.TestScoreTaskMessage;
import com.example.common.redis.RedisComponent;
import com.example.common.utils.CopyTools;
import com.example.wx.elasticsearch.service.AIEvaluationService;
import com.example.wx.elasticsearch.service.ElasticsearchSyncService;
import com.example.wx.mapper.AudioMapper;
import com.example.wx.mapper.TestdetailMapper;
import com.example.wx.pojo.Audio;
import com.example.wx.pojo.Testdetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestScoreTaskConsumer {

    private final TestdetailMapper testdetailMapper;
    private final AudioMapper audioMapper;
    private final ElasticsearchSyncService elasticsearchSyncService;
    private final AIEvaluationService aiEvaluationService;
    private final RedisComponent redisComponent;

    @Scheduled(fixedDelay = 500, timeUnit = java.util.concurrent.TimeUnit.MILLISECONDS)
    public void consumeTasks() {
        TestScoreTaskMessage message;
        while ((message = redisComponent.rpopScoreTask()) != null) {
            try {
                processTask(message);
            } catch (Exception e) {
                log.error("处理评分任务失败: testDetailId={}, error={}", message.getTestDetailId(), e.getMessage(), e);
            }
        }
    }

    private void processTask(TestScoreTaskMessage msg) {
        Testdetail testdetail = testdetailMapper.selectById(msg.getTestDetailId());
        if (testdetail == null) {
            log.warn("测试详情不存在: {}", msg.getTestDetailId());
            return;
        }

        String userContent = msg.getUserContent();
        String audioContent = msg.getAudioContent();

        float durationScore = calculateDurationScore(msg.getUserDuration(), msg.getStandardDuration());
        float similarityScore = calculateEditDistanceScore(userContent, audioContent);

        AIEvaluationService.AIEvaluationResult aiResult = null;
        try {
            aiResult = aiEvaluationService.evaluate(userContent, audioContent);
        } catch (Exception e) {
            log.warn("AI评分调用失败: {}", e.getMessage());
        }

        /*
         * 相似度 100%：总分 100，不调 AI（evaluate 内已短路）。
         * 否则：总分 = 文本相似度×0.5 + AI 返回 score×0.3 + 时长得分×0.2；AI 未返回 score 时用相似度兜底 0.3 项。
         */
        float finalScore = 0f;
        float aiRawScore = 0f;
        if (similarityScore >= 100f) {
            finalScore = 100f;
            aiRawScore = 100f;
        } else {
            float aiRaw;
            if (aiResult != null && aiResult.isAiRawScorePresent()) {
                aiRaw = Math.max(0f, Math.min(100f, aiResult.getScore()));
            } else {
                aiRaw = similarityScore;
            }
            finalScore = similarityScore * 0.5f + aiRaw * 0.3f + durationScore * 0.2f;
        }
        finalScore = Math.max(0f, Math.min(100f, finalScore));

        String userText = userContent != null ? userContent.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "") : "";
        String audioText = audioContent != null ? audioContent.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "") : "";

        String errorPositions = "";
        String errorTags = "";
        String resultAnalysis = "";

        if (similarityScore >= 100f) {
            if (aiResult != null && aiResult.getResultAnalysis() != null && !aiResult.getResultAnalysis().isEmpty()) {
                resultAnalysis = aiResult.getResultAnalysis();
            }
        } else if (finalScore < 100f) {
            errorPositions = calculateErrorPositions(userText, audioText);

            if (aiResult != null && aiResult.getErrorTags() != null && !aiResult.getErrorTags().isEmpty()) {
                errorTags = aiResult.getErrorTags();
            } else {
                errorTags = analyzeErrorTagsLocal(userText, audioText);
            }

            StringBuilder analysisSb = new StringBuilder();
            if (aiResult != null && aiResult.getErrorDetail() != null && !aiResult.getErrorDetail().isBlank()) {
                analysisSb.append("错误对比：").append(aiResult.getErrorDetail().trim());
            }
            if (aiResult != null && aiResult.getResultAnalysis() != null && !aiResult.getResultAnalysis().isEmpty()) {
                if (analysisSb.length() > 0) {
                    analysisSb.append('\n');
                }
                analysisSb.append(aiResult.getResultAnalysis().trim());
            }
            if (analysisSb.length() > 0) {
                resultAnalysis = analysisSb.toString();
            }
        }

        testdetail.setScore(finalScore);
        testdetail.setErrorPositions(errorPositions);
        testdetail.setErrorTags(errorTags);
        testdetail.setResultAnalysis(resultAnalysis);
        testdetail.setDurationScore(durationScore);
        testdetail.setEditDistanceScore(similarityScore);
        testdetail.setAiScore(aiRawScore);
        testdetailMapper.updateById(testdetail);
        redisComponent.deleteAudio(msg.getAudioId());
        redisComponent.deleteTestDetailCache(testdetail.getTestId());

        Audio audio = audioMapper.selectById(msg.getAudioId());
        TestDto testDto = CopyTools.copy(testdetail, TestDto.class);
        if (audio != null) {
            testDto.setAudioPath(getHttpAudio.getAudioUrl(audio.getPath()));
        }
        testDto.setTestAudioPath(getHttpAudio.getAudioUrl(msg.getUserAudioPath()));
        testDto.setScore(finalScore);

        elasticsearchSyncService.syncTestItemToEs(testDto);
        log.info("异步评分完成: testDetailId={}, score={}", msg.getTestDetailId(), finalScore);
    }

    private float calculateDurationScore(float userDuration, float standardDuration) {
        if (standardDuration <= 0) return 100f;
        float ratio = userDuration / standardDuration;
        float score;
        if (ratio >= 0.8f && ratio <= 1.2f) {
            score = 100f;
        } else if (ratio < 0.8f) {
            score = 100f * (userDuration / (standardDuration * 0.8f));
        } else {
            score = 100f * (1f - (ratio - 1.2f) / 1.2f);
        }
        return Math.max(0f, Math.min(100f, score));
    }

    private float calculateEditDistanceScore(String userContent, String audioContent) {
        if (userContent == null || audioContent == null) return 0;
        String userText = userContent.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");
        String audioText = audioContent.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");
        int distance = levenshteinDistance(userText, audioText);
        int maxLength = Math.max(userText.length(), audioText.length());
        if (maxLength == 0) return 100f;
        return (float) ((1 - (double) distance / maxLength) * 100);
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]) + 1;
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }

    private String calculateErrorPositions(String userText, String audioText) {
        List<Integer> errorPositions = new ArrayList<>();
        int minLen = Math.min(userText.length(), audioText.length());
        for (int i = 0; i < minLen; i++) {
            if (userText.charAt(i) != audioText.charAt(i)) {
                errorPositions.add(i + 1);
            }
        }
        if (userText.length() > audioText.length()) {
            for (int i = audioText.length(); i < userText.length(); i++) {
                errorPositions.add(i + 1);
            }
        } else if (audioText.length() > userText.length()) {
            for (int i = userText.length(); i < audioText.length(); i++) {
                errorPositions.add(i + 1);
            }
        }
        if (errorPositions.isEmpty()) return "";
        return errorPositions.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private String analyzeErrorTagsLocal(String userText, String audioText) {
        List<String> errorTags = new ArrayList<>();
        if (containsAny(userText, "zh,ch,sh") && containsAny(audioText, "z,c,s") ||
            containsAny(audioText, "zh,ch,sh") && containsAny(userText, "z,c,s")) {
            errorTags.add("平翘舌");
        }
        if (containsAny(userText, "ing,eng") && containsAny(audioText, "in,en") ||
            containsAny(audioText, "ing,eng") && containsAny(userText, "in,en")) {
            errorTags.add("前后鼻音");
        }
        if (containsAny(userText, "n") && containsAny(audioText, "l") ||
            containsAny(audioText, "n") && containsAny(userText, "l")) {
            errorTags.add("nl混淆");
        }
        if (containsAny(userText, "h") && containsAny(audioText, "f") ||
            containsAny(audioText, "h") && containsAny(userText, "f")) {
            errorTags.add("hf混淆");
        }
        if (containsAny(userText, "b,d,g") && containsAny(audioText, "p,t,k") ||
            containsAny(audioText, "b,d,g") && containsAny(userText, "p,t,k")) {
            errorTags.add("送气音");
        }
        if (errorTags.isEmpty()) {
            errorTags.add("发音错误");
        }
        return String.join(",", errorTags);
    }

    private boolean containsAny(String text, String chars) {
        for (char c : chars.replace(",", "").toCharArray()) {
            if (text.indexOf(c) >= 0) return true;
        }
        return false;
    }
}
