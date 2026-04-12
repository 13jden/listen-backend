package com.example.admin.controller;

import com.example.common.common.Result;
import com.example.wx.pojo.UserPhaseReport;
import com.example.wx.service.UserPhaseReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户阶段性报告Controller
 */
@Tag(name = "用户阶段性报告接口")
@RestController
@RequestMapping("/admin/phase-report")
@RequiredArgsConstructor
public class UserPhaseReportController {

    @Autowired
    private UserPhaseReportService userPhaseReportService;

    @Operation(summary = "根据userId查询阶段性报告")
    @GetMapping("/getByUserId")
    public Result getReportByUserId(
            @Parameter(description = "用户ID") @RequestParam String userId) {
        UserPhaseReport report = userPhaseReportService.getReportByUserId(userId);
        if (report == null) {
            return Result.error("报告不存在");
        }
        return Result.success(report);
    }

    @Operation(summary = "生成/更新阶段性报告（AI分析）")
    @PostMapping("/generate")
    public Result<String> generateReport(
            @Parameter(description = "用户ID") @RequestParam String userId) {
        userPhaseReportService.generateReport(userId);
        return Result.success("报告生成成功");
    }

    @Operation(summary = "保存医生评语和建议")
    @PostMapping("/doctor-advice")
    public Result<String> saveDoctorAdvice(
            @Parameter(description = "用户ID") @RequestParam String userId,
            @Parameter(description = "医生评语") @RequestParam(required = false) String doctorComment,
            @Parameter(description = "医生建议") @RequestParam(required = false) String doctorSuggestion) {
        userPhaseReportService.saveDoctorAdvice(userId, doctorComment, doctorSuggestion);
        return Result.success("保存成功");
    }
}
