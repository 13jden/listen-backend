package com.example.wx.elasticsearch.service;

import com.example.common.api.BailianKnowledgeService;
import com.example.wx.elasticsearch.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 每日分析定时任务
 * 1. 每日凌晨更新Redis缓存数据
 * 2. 每日生成AI分析报告并同步到知识库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyAnalysisTask {

    @Autowired
    private ElasticsearchAggregationService aggregationService;

    @Autowired
    private AIAnalysisService aiAnalysisService;

    @Autowired
    private BailianKnowledgeService bailianKnowledgeService;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private static final String DAILY_STATS_KEY = "listen:daily:stats:";
    private static final String MONTHLY_REPORT_KEY = "listen:monthly:report:";

    /**
     * 每日凌晨2点执行：更新Redis缓存数据
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void updateDailyStats() {
        log.info("开始执行每日数据统计任务...");
        try {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDate yesterday = LocalDate.now().minusDays(1);
            String yesterdayStr = yesterday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // 1. 更新每日统计数据
            updateDailyStatsToRedis(yesterdayStr);

            // 2. 更新月度统计数据
            String month = yesterday.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            updateMonthlyStatsToRedis(month);

            // 3. 更新总计统计数据
            updateSummaryStatsToRedis();

            log.info("每日数据统计任务完成");
        } catch (Exception e) {
            log.error("每日数据统计任务失败", e);
        }
    }

    /**
     * 每日凌晨3点执行：生成AI月度报告并同步到知识库
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void generateMonthlyAIReport() {
        log.info("开始执行AI月度报告生成任务...");
        try {
            // 生成上个月的报告
            LocalDate lastMonth = LocalDate.now().minusMonths(1);
            String month = lastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));

            log.info("生成 {} 月度报告", month);

            // 1. 调用AI生成月度报告
            String report = aiAnalysisService.generateMonthlyReport(month);

            // 2. 同步到知识库
            String fileId = bailianKnowledgeService.uploadFileToKnowledgeBase(
                    month + " 月度听力康复分析报告",
                    report
            );

            if (fileId != null) {
                log.info("月度报告已同步到知识库，fileId: {}", fileId);
                // 缓存报告ID
                saveMonthlyReportToRedis(month, fileId, report);
            } else {
                log.warn("月度报告同步到知识库失败，报告内容已缓存");
                // 即使上传失败，也缓存报告内容
                saveMonthlyReportToRedis(month, null, report);
            }

            log.info("AI月度报告生成任务完成");
        } catch (Exception e) {
            log.error("AI月度报告生成任务失败", e);
        }
    }

    /**
     * 每日凌晨4点执行：生成各医院分析报告
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void generateHospitalReports() {
        log.info("开始执行医院报告生成任务...");
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            String startDate = yesterday.format(DateTimeFormatter.ofPattern("yyyy-MM-01"));
            String endDate = yesterday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // 获取所有医院统计数据
            HospitalStatsVO hospitalStats = aggregationService.getHospitalStats(startDate, endDate);

            if (hospitalStats != null && hospitalStats.getData() != null) {
                for (HospitalStatsVO.HospitalItem hospital : hospitalStats.getData()) {
                    try {
                        // 生成每个医院的报告
                        String report = aiAnalysisService.generateHospitalReport(
                                hospital.getHospitalName(),
                                hospital.getHospitalName(),
                                startDate,
                                endDate
                        );

                        // 同步到知识库
                        String fileId = bailianKnowledgeService.uploadFileToKnowledgeBase(
                                hospital.getHospitalName() + " 听力康复评估报告",
                                report
                        );

                        log.info("医院 {} 报告已生成，fileId: {}", hospital.getHospitalName(), fileId);

                        // 避免请求过快
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        log.error("生成医院 {} 报告失败", hospital.getHospitalName(), e);
                    }
                }
            }

            log.info("医院报告生成任务完成");
        } catch (Exception e) {
            log.error("医院报告生成任务失败", e);
        }
    }

    /**
     * 每周一凌晨5点执行：生成老年群体专项分析
     */
    @Scheduled(cron = "0 0 5 ? * MON")
    public void generateElderlyAnalysis() {
        log.info("开始执行老年群体专项分析任务...");
        try {
            LocalDate today = LocalDate.now();
            String startDate = today.minusDays(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String endDate = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // 生成老年群体专项分析
            String report = aiAnalysisService.generateElderlyAnalysis(startDate, endDate);

            // 同步到知识库
            String fileId = bailianKnowledgeService.uploadFileToKnowledgeBase(
                    "老年群体听力康复专项分析_" + endDate,
                    report
            );

            if (fileId != null) {
                log.info("老年群体专项分析已同步到知识库，fileId: {}", fileId);
            }

            log.info("老年群体专项分析任务完成");
        } catch (Exception e) {
            log.error("老年群体专项分析任务失败", e);
        }
    }

    /**
     * 更新每日统计数据到Redis
     */
    public void updateDailyStatsToRedis(String date) {
        if (redisTemplate == null) {
            log.warn("Redis未配置，跳过每日统计更新");
            return;
        }

        try {
            DailyReportVO dailyReport = aggregationService.getDailyReport(date, date);

            Map<String, String> stats = new HashMap<>();
            stats.put("date", date);
            stats.put("data", new com.google.gson.Gson().toJson(dailyReport));

            redisTemplate.opsForHash().putAll(DAILY_STATS_KEY + date, stats);
            // 设置7天过期
            redisTemplate.expire(DAILY_STATS_KEY + date, Duration.ofDays(7));

            log.info("每日统计数据已更新到Redis: {}", date);
        } catch (Exception e) {
            log.error("更新每日统计数据到Redis失败", e);
        }
    }

    /**
     * 更新月度统计数据到Redis
     */
    public void updateMonthlyStatsToRedis(String month) {
        if (redisTemplate == null) {
            log.warn("Redis未配置，跳过月度统计更新");
            return;
        }

        try {
            String startDate = month + "-01";
            String endDate = month + "-31";

            // 获取月度统计数据
            MonthlyTrendVO trend = aggregationService.getMonthlyTrend(startDate, endDate);
            SummaryStatsVO summary = aggregationService.getSummaryStats();
            ScoreDistributionVO scoreDist = aggregationService.getScoreDistribution(startDate, endDate);

            Map<String, String> stats = new HashMap<>();
            stats.put("month", month);
            stats.put("trend", new com.google.gson.Gson().toJson(trend));
            stats.put("summary", new com.google.gson.Gson().toJson(summary));
            stats.put("scoreDistribution", new com.google.gson.Gson().toJson(scoreDist));

            redisTemplate.opsForHash().putAll(MONTHLY_REPORT_KEY + month, stats);
            // 设置30天过期
            redisTemplate.expire(MONTHLY_REPORT_KEY + month, Duration.ofDays(30));

            log.info("月度统计数据已更新到Redis: {}", month);
        } catch (Exception e) {
            log.error("更新月度统计数据到Redis失败", e);
        }
    }

    /**
     * 更新总计统计数据到Redis
     */
    public void updateSummaryStatsToRedis() {
        if (redisTemplate == null) {
            log.warn("Redis未配置，跳过总计统计更新");
            return;
        }

        try {
            SummaryStatsVO summary = aggregationService.getSummaryStats();
            String summaryJson = new com.google.gson.Gson().toJson(summary);

            redisTemplate.opsForValue().set("listen:summary:stats", summaryJson);
            // 设置24小时过期
            redisTemplate.expire("listen:summary:stats", Duration.ofHours(24));

            log.info("总计统计数据已更新到Redis");
        } catch (Exception e) {
            log.error("更新总计统计数据到Redis失败", e);
        }
    }

    /**
     * 保存月度报告到Redis
     */
    private void saveMonthlyReportToRedis(String month, String fileId, String report) {
        if (redisTemplate == null) {
            return;
        }

        try {
            Map<String, String> reportData = new HashMap<>();
            reportData.put("month", month);
            reportData.put("fileId", fileId != null ? fileId : "");
            reportData.put("report", report);
            reportData.put("generatedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

            redisTemplate.opsForHash().putAll(MONTHLY_REPORT_KEY + month + ":report", reportData);
            redisTemplate.expire(MONTHLY_REPORT_KEY + month + ":report", Duration.ofDays(365));

            log.info("月度报告已缓存到Redis: {}", month);
        } catch (Exception e) {
            log.error("保存月度报告到Redis失败", e);
        }
    }

    /**
     * 从Redis获取每日统计数据
     */
    public DailyReportVO getDailyStatsFromRedis(String date) {
        if (redisTemplate == null) {
            return null;
        }

        try {
            Object data = redisTemplate.opsForHash().get(DAILY_STATS_KEY + date, "data");
            if (data != null) {
                return new com.google.gson.Gson().fromJson(data.toString(), DailyReportVO.class);
            }
        } catch (Exception e) {
            log.error("从Redis获取每日统计数据失败", e);
        }
        return null;
    }

    /**
     * 从Redis获取月度报告
     */
    public Map<String, String> getMonthlyReportFromRedis(String month) {
        if (redisTemplate == null) {
            return null;
        }

        try {
            Map<Object, Object> reportData = redisTemplate.opsForHash().entries(MONTHLY_REPORT_KEY + month + ":report");
            if (reportData != null && !reportData.isEmpty()) {
                Map<String, String> result = new HashMap<>();
                reportData.forEach((k, v) -> result.put(k.toString(), v.toString()));
                return result;
            }
        } catch (Exception e) {
            log.error("从Redis获取月度报告失败", e);
        }
        return null;
    }

    /**
     * 从Redis获取总计统计
     */
    public SummaryStatsVO getSummaryStatsFromRedis() {
        if (redisTemplate == null) {
            return null;
        }

        try {
            Object data = redisTemplate.opsForValue().get("listen:summary:stats");
            if (data != null) {
                return new com.google.gson.Gson().fromJson(data.toString(), SummaryStatsVO.class);
            }
        } catch (Exception e) {
            log.error("从Redis获取总计统计失败", e);
        }
        return null;
    }

    /**
     * 手动触发每日统计更新
     */
    public void triggerDailyUpdate() {
        log.info("手动触发每日数据更新");
        updateDailyStats();
    }

    /**
     * 手动触发月度报告生成
     */
    public void triggerMonthlyReport(String month) {
        log.info("手动触发月度报告生成: {}", month);
        try {
            String report = aiAnalysisService.generateMonthlyReport(month);
            String fileId = bailianKnowledgeService.uploadFileToKnowledgeBase(
                    month + " 月度听力康复分析报告",
                    report
            );
            saveMonthlyReportToRedis(month, fileId, report);
        } catch (Exception e) {
            log.error("手动生成月度报告失败", e);
        }
    }
}
