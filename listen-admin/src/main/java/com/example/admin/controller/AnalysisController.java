package com.example.admin.controller;

import com.example.common.common.Result;
import com.example.wx.elasticsearch.service.DailyAnalysisTask;
import com.example.wx.elasticsearch.service.ElasticsearchAggregationService;
import com.example.wx.elasticsearch.service.ElasticsearchQueryService;
import com.example.wx.elasticsearch.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数据分析Controller
 * 提供各种统计分析接口
 */
@Tag(name = "数据分析接口")
@RestController
@RequestMapping("/admin/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final ElasticsearchAggregationService aggregationService;
    private final ElasticsearchQueryService queryService;
    private final DailyAnalysisTask dailyAnalysisTask;

    @Operation(summary = "测试年龄分布")
    @GetMapping("/age-distribution")
    public Result<AgeGroupDistributionVO> getAgeDistribution(
            @Parameter(description = "开始日期 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        AgeGroupDistributionVO cached = dailyAnalysisTask.getAgeDistributionFromRedis();
        if (cached != null) {
            return Result.success(cached);
        }
        return Result.success(aggregationService.getAgeGroupDistribution(startDate, endDate));
    }

    @Operation(summary = "月度测试趋势")
    @GetMapping("/monthly-trend")
    public Result<MonthlyTrendVO> getMonthlyTrend(
            @Parameter(description = "开始日期 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        MonthlyTrendVO cached = dailyAnalysisTask.getMonthlyTrendFromRedis();
        if (cached != null) {
            return Result.success(cached);
        }
        return Result.success(aggregationService.getMonthlyTrend(startDate, endDate));
    }

    @Operation(summary = "测试完成情况")
    @GetMapping("/completion-status")
    public Result<CompletionStatusVO> getCompletionStatus(
            @Parameter(description = "开始日期 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        CompletionStatusVO cached = dailyAnalysisTask.getCompletionStatusFromRedis();
        if (cached != null) {
            return Result.success(cached);
        }
        return Result.success(aggregationService.getCompletionStatus(startDate, endDate));
    }

    @Operation(summary = "错误类型分布")
    @GetMapping("/error-type")
    public Result<ErrorTypeDistributionVO> getErrorTypeDistribution(
            @Parameter(description = "开始日期 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        ErrorTypeDistributionVO cached = dailyAnalysisTask.getErrorTypeDistributionFromRedis();
        if (cached != null) {
            return Result.success(cached);
        }
        return Result.success(aggregationService.getErrorTypeDistribution(startDate, endDate));
    }

    @Operation(summary = "医院测试统计")
    @GetMapping("/hospital-stats")
    public Result<HospitalStatsVO> getHospitalStats(
            @Parameter(description = "开始日期 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        HospitalStatsVO cached = dailyAnalysisTask.getHospitalStatsFromRedis();
        if (cached != null) {
            return Result.success(cached);
        }
        return Result.success(aggregationService.getHospitalStats(startDate, endDate));
    }

    @Operation(summary = "测试得分分布")
    @GetMapping("/score-distribution")
    public Result<ScoreDistributionVO> getScoreDistribution(
            @Parameter(description = "开始日期 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        ScoreDistributionVO cached = dailyAnalysisTask.getScoreDistributionFromRedis();
        if (cached != null) {
            return Result.success(cached);
        }
        return Result.success(aggregationService.getScoreDistribution(startDate, endDate));
    }

    @Operation(summary = "每日数据报表")
    @GetMapping("/daily-report")
    public Result<DailyReportVO> getDailyReport(
            @Parameter(description = "开始日期 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        // 单日查询优先从Redis获取，没有则查ES并回填
        if (startDate != null && startDate.equals(endDate)) {
            DailyReportVO cached = dailyAnalysisTask.getDailyStatsFromRedis(startDate);
            if (cached != null) {
                return Result.success(cached);
            }
            DailyReportVO vo = aggregationService.getDailyReport(startDate, endDate);
            if (vo != null && vo.getData() != null && !vo.getData().isEmpty()) {
                dailyAnalysisTask.updateDailyStatsToRedis(startDate, vo);
            }
            return Result.success(vo);
        }
        return Result.success(aggregationService.getDailyReport(startDate, endDate));
    }

    @Operation(summary = "统计总计")
    @GetMapping("/summary")
    public Result<SummaryStatsVO> getSummaryStats() {
        SummaryStatsVO cached = dailyAnalysisTask.getSummaryStatsFromRedis();
        if (cached != null) {
            return Result.success(cached);
        }
        SummaryStatsVO vo = aggregationService.getSummaryStats();
        dailyAnalysisTask.updateSummaryStatsToRedis();
        return Result.success(vo);
    }

    @Operation(summary = "Dashboard卡片数据")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboardStats(
            @Parameter(description = "开始日期 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        return Result.success(queryService.getDashboardStats(startDate, endDate));
    }

    @Operation(summary = "年龄组听力表现分布（旧接口兼容）")
    @GetMapping("/age-group-distribution")
    public Result<Map<String, Object>> getAgeGroupDistribution(
            @Parameter(description = "开始日期 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        return Result.success(queryService.getAgeGroupDistribution(startDate, endDate));
    }
}
