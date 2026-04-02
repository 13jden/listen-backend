package com.example.admin.controller;

import com.example.common.common.Result;
import com.example.wx.elasticsearch.service.AIAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * AI辅助分析Controller
 */
@Tag(name = "AI辅助分析接口")
@RestController
@RequestMapping("/admin/ai-analysis")
@RequiredArgsConstructor
public class AIAnalysisController {

    private final AIAnalysisService aiAnalysisService;

    @Operation(summary = "AI综合分析")
    @PostMapping("/analyze")
    public Result<AIAnalysisService.AnalysisResult> analyze(
            @RequestBody AIAnalysisService.AnalysisRequest request) {
        return Result.success(aiAnalysisService.analyze(request));
    }

    @Operation(summary = "生成月度报告")
    @GetMapping("/monthly-report")
    public Result<String> generateMonthlyReport(
            @Parameter(description = "月份 yyyy-MM") @RequestParam String month) {
        String report = aiAnalysisService.generateMonthlyReport(month);
        return Result.success(report);
    }

    @Operation(summary = "生成医院分析报告")
    @GetMapping("/hospital-report")
    public Result<String> generateHospitalReport(
            @Parameter(description = "医院ID") @RequestParam String hospitalId,
            @Parameter(description = "医院名称") @RequestParam String hospitalName,
            @Parameter(description = "开始日期 yyyy-MM-dd") @RequestParam String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam String endDate) {
        String report = aiAnalysisService.generateHospitalReport(hospitalId, hospitalName, startDate, endDate);
        return Result.success(report);
    }

    @Operation(summary = "生成老年群体专项分析")
    @GetMapping("/elderly-analysis")
    public Result<String> generateElderlyAnalysis(
            @Parameter(description = "开始日期 yyyy-MM-dd") @RequestParam String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam String endDate) {
        String report = aiAnalysisService.generateElderlyAnalysis(startDate, endDate);
        return Result.success(report);
    }
}
