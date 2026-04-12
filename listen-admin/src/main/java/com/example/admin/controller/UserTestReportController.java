package com.example.admin.controller;

import com.example.common.common.Result;
import com.example.wx.pojo.UserPhaseReport;
import com.example.wx.pojo.UserTestReport;
import com.example.wx.service.UserPhaseReportService;
import com.example.wx.service.UserTestReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户测试报告Controller
 */
@Tag(name = "用户测试报告接口")
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class UserTestReportController {

    @Autowired
    private UserTestReportService userTestReportService;

    @Autowired
    private UserPhaseReportService userPhaseReportService;

    @Operation(summary = "根据testId查询测试报告")
    @GetMapping("/getByTestId")
    public Result getReportByTestId(
            @Parameter(description = "测试ID") @RequestParam String testId) {
        UserTestReport report = userTestReportService.getReportByTestId(testId);
        if (report == null) {
            return Result.error("报告不存在");
        }
        return Result.success(report);
    }



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

}
