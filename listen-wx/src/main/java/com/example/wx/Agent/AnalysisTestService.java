package com.example.wx.Agent;

import com.example.wx.elasticsearch.service.DailyAnalysisTask;
import com.example.wx.elasticsearch.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 分析数据测试 Service
 * 模拟填充 Redis 缓存数据，便于前端展示统计图表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisTestService {

    private final DailyAnalysisTask dailyAnalysisTask;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Random RANDOM = new Random(42);

    public String fillAllMockData() {
        fillSummaryStats();
        fillDailyReports();
        fillMonthlyTrend();
        fillAgeDistribution();
        fillCompletionStatus();
        fillErrorTypeDistribution();
        fillHospitalStats();
        fillScoreDistribution();
        return "所有模拟数据已填充到Redis";
    }

    public String fillSummaryStats() {
        SummaryStatsVO vo = new SummaryStatsVO();
        vo.setTotalUsers(1268L);
        vo.setTotalTests(3847L);
        vo.setAvgScore(72.5);
        vo.setScoreImprovement(3.2);
        if (dailyAnalysisTask.updateSummaryStatsToRedis(vo)) {
            log.info("总计统计数据已填充到Redis");
            return "总计统计数据已填充";
        }
        return "Redis未配置，跳过";
    }

    public String fillDailyReports() {
        LocalDate today = LocalDate.now();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(DF);

            DailyReportVO vo = new DailyReportVO();
            List<DailyReportVO.DailyReportItem> items = new ArrayList<>();
            DailyReportVO.DailyReportItem item = new DailyReportVO.DailyReportItem();
            item.setDate(dateStr);

            int dayFactor = 30 - i;
            long userCount = 20 + dayFactor * 3 + RANDOM.nextInt(10);
            long testCount = userCount + RANDOM.nextInt(15);
            double avgScore = 62.0 + dayFactor * 0.4 + RANDOM.nextDouble() * 5;
            double completionRate = 75.0 + RANDOM.nextDouble() * 12;

            item.setUserCount(userCount);
            item.setTestCount(testCount);
            item.setAvgScore(Math.round(avgScore * 10.0) / 10.0);
            item.setCompletionRate(Math.round(completionRate * 10.0) / 10.0);
            items.add(item);
            vo.setData(items);

            dailyAnalysisTask.updateDailyStatsToRedis(dateStr, vo);
        }
        log.info("最近30天每日报表数据已填充到Redis");
        return "最近30天每日报表数据已填充";
    }

    public String fillMonthlyTrend() {
        MonthlyTrendVO vo = new MonthlyTrendVO();
        List<String> months = new ArrayList<>();
        List<Long> testCounts = new ArrayList<>();
        List<Double> avgScores = new ArrayList<>();

        LocalDate today = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate month = today.minusMonths(i);
            String monthStr = month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            months.add(monthStr);

            long count = 380L + (5 - i) * 140 + RANDOM.nextInt(80);
            double avg = 65.0 + (5 - i) * 1.5 + RANDOM.nextDouble() * 3;
            testCounts.add(count);
            avgScores.add(Math.round(avg * 10.0) / 10.0);
        }
        vo.setMonths(months);
        vo.setTestCounts(testCounts);
        vo.setAvgScores(avgScores);

        if (dailyAnalysisTask.updateMonthlyTrendToRedis(vo)) {
            log.info("月度测试趋势数据已填充到Redis");
            return "月度测试趋势数据已填充";
        }
        return "Redis未配置，跳过";
    }

    public String fillAgeDistribution() {
        AgeGroupDistributionVO vo = new AgeGroupDistributionVO();
        List<AgeGroupDistributionVO.AgeGroupItem> items = new ArrayList<>();

        String[] groups = {"<55", "55-60", "60-65", "65-70", ">=70"};
        double[] avgs = {78.5, 74.2, 71.8, 68.3, 63.5};
        double[] maxs = {98.0, 96.0, 94.0, 91.0, 88.0};
        double[] mins = {52.0, 48.0, 45.0, 42.0, 38.0};
        long[] counts = {320L, 285L, 310L, 220L, 133L};

        for (int i = 0; i < groups.length; i++) {
            AgeGroupDistributionVO.AgeGroupItem item = new AgeGroupDistributionVO.AgeGroupItem();
            item.setAgeGroup(groups[i]);
            item.setAvgScore(avgs[i] + RANDOM.nextDouble() * 2 - 1);
            item.setMaxScore(maxs[i] + RANDOM.nextDouble() * 2 - 1);
            item.setMinScore(mins[i] + RANDOM.nextDouble() * 2 - 1);
            item.setCount(counts[i] + RANDOM.nextInt(20) - 10);
            items.add(item);
        }
        vo.setData(items);

        if (dailyAnalysisTask.updateAgeDistributionToRedis(vo)) {
            log.info("年龄分布数据已填充到Redis");
            return "年龄分布数据已填充";
        }
        return "Redis未配置，跳过";
    }

    public String fillCompletionStatus() {
        CompletionStatusVO vo = new CompletionStatusVO();
        vo.setCompleted(3200L);
        vo.setInProgress(450L);
        vo.setNotStarted(197L);

        if (dailyAnalysisTask.updateCompletionStatusToRedis(vo)) {
            log.info("完成状态数据已填充到Redis");
            return "完成状态数据已填充";
        }
        return "Redis未配置，跳过";
    }

    public String fillErrorTypeDistribution() {
        ErrorTypeDistributionVO vo = new ErrorTypeDistributionVO();
        List<ErrorTypeDistributionVO.ErrorTypeItem> items = new ArrayList<>();

        String[] types = {"高频音辨识困难", "低频音区分障碍", "噪声环境下识别差", "言语识别困难", "双耳协调问题", "时序辨别障碍", "音调记忆衰退"};
        long[] counts = {890L, 756L, 645L, 523L, 412L, 318L, 256L};

        for (int i = 0; i < types.length; i++) {
            ErrorTypeDistributionVO.ErrorTypeItem item = new ErrorTypeDistributionVO.ErrorTypeItem();
            item.setErrorType(types[i]);
            item.setCount(counts[i] + RANDOM.nextInt(50) - 25);
            items.add(item);
        }
        vo.setData(items);

        if (dailyAnalysisTask.updateErrorTypeDistributionToRedis(vo)) {
            log.info("错误类型分布数据已填充到Redis");
            return "错误类型分布数据已填充";
        }
        return "Redis未配置，跳过";
    }

    public String fillHospitalStats() {
        HospitalStatsVO vo = new HospitalStatsVO();
        List<HospitalStatsVO.HospitalItem> items = new ArrayList<>();

        String[] hospitals = {"北京协和医院", "上海瑞金医院", "广州中山医院", "深圳北大医院", "成都华西医院"};
        long[] users = {380L, 295L, 260L, 215L, 118L};
        long[] tests = {1200L, 890L, 780L, 640L, 337L};
        double[] avgs = {76.5, 73.8, 71.2, 69.5, 67.8};

        for (int i = 0; i < hospitals.length; i++) {
            HospitalStatsVO.HospitalItem item = new HospitalStatsVO.HospitalItem();
            item.setHospitalName(hospitals[i]);
            item.setUserCount(users[i] + RANDOM.nextInt(20) - 10);
            item.setTestCount(tests[i] + RANDOM.nextInt(50) - 25);
            item.setAvgScore(avgs[i] + RANDOM.nextDouble() * 2 - 1);
            items.add(item);
        }
        vo.setData(items);

        if (dailyAnalysisTask.updateHospitalStatsToRedis(vo)) {
            log.info("医院统计数据已填充到Redis");
            return "医院统计数据已填充";
        }
        return "Redis未配置，跳过";
    }

    public String fillScoreDistribution() {
        ScoreDistributionVO vo = new ScoreDistributionVO();
        vo.setRanges(Arrays.asList("0-20", "20-40", "40-60", "60-80", "80-100"));
        vo.setCounts(Arrays.asList(45L, 185L, 620L, 1850L, 1147L));

        if (dailyAnalysisTask.updateScoreDistributionToRedis(vo)) {
            log.info("得分分布数据已填充到Redis");
            return "得分分布数据已填充";
        }
        return "Redis未配置，跳过";
    }
}
