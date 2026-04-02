package com.example.admin.controller;

import com.example.common.common.Result;
import com.example.wx.elasticsearch.service.DailyAnalysisTask;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数据报表Controller
 * 从Redis缓存获取数据报表
 */
@Tag(name = "数据报表接口")
@RestController
@RequestMapping("/admin/report")
@RequiredArgsConstructor
public class ReportController {

    private final DailyAnalysisTask dailyAnalysisTask;

    @Operation(summary = "获取每日报表(从Redis)")
    @GetMapping("/daily")
    public Result<?> getDailyReport(
            @Parameter(description = "日期 yyyy-MM-dd") @RequestParam String date) {
        // 先从Redis获取
        var report = dailyAnalysisTask.getDailyStatsFromRedis(date);
        if (report != null) {
            return Result.success(report);
        }
        // Redis没有则重新计算
        dailyAnalysisTask.triggerDailyUpdate();
        report = dailyAnalysisTask.getDailyStatsFromRedis(date);
        return Result.success(report);
    }

    @Operation(summary = "获取月度报告(从Redis)")
    @GetMapping("/monthly")
    public Result<Map<String, String>> getMonthlyReport(
            @Parameter(description = "月份 yyyy-MM") @RequestParam String month) {
        var report = dailyAnalysisTask.getMonthlyReportFromRedis(month);
        if (report != null && !report.isEmpty()) {
            return Result.success(report);
        }
        // Redis没有则手动触发生成
        dailyAnalysisTask.triggerMonthlyReport(month);
        report = dailyAnalysisTask.getMonthlyReportFromRedis(month);
        return Result.success(report);
    }

    @Operation(summary = "获取总计统计")
    @GetMapping("/summary")
    public Result<?> getSummaryStats() {
        var stats = dailyAnalysisTask.getSummaryStatsFromRedis();
        if (stats != null) {
            return Result.success(stats);
        }
        return Result.success("无缓存数据，请稍后重试");
    }

    @Operation(summary = "手动触发每日统计更新")
    @PostMapping("/trigger-daily-update")
    public Result<String> triggerDailyUpdate() {
        dailyAnalysisTask.triggerDailyUpdate();
        return Result.success("已触发每日统计更新");
    }
}
