package com.example.admin.controller;

import com.example.wx.Agent.AnalysisTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分析数据测试 Controller
 * 用于模拟填充 Redis 缓存数据，便于前端展示统计图表
 */
@Tag(name = "分析数据测试接口")
@RestController
@RequestMapping("/admin/analysis/test")
@RequiredArgsConstructor
public class AnalysisTestController {

    private final AnalysisTestService analysisTestService;

    @Operation(summary = "填充所有模拟数据到Redis")
    @PostMapping("/fill")
    public String fillAllMockData() {
        return analysisTestService.fillAllMockData();
    }
}
