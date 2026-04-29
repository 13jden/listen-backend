package com.example.admin.controller;


import com.example.common.common.Result;
import com.example.wx.pojo.UserPhaseReport;
import com.example.wx.service.AudioService;
import com.example.wx.service.UserPhaseReportService;
import com.example.wx.service.UsertestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户测试接口")
@RestController
@RequestMapping("/usertest")
public class UsertestController {

    @Autowired
    private AudioService audioService;

    @Autowired
    private UsertestService usertestService;

    @Operation(summary = "获取测试题目", description = "创建测试并获取测试题目列表")
    @GetMapping("getTest")
    public Result getTest(@Parameter(description = "用户ID") @RequestParam String userId,
                          @Parameter(description = "题目数量") @RequestParam int num,
                          @Parameter(description = "是否继续") @RequestParam boolean isContinue,
                          @Parameter(description = "时间戳") @RequestParam int time) {
        return usertestService.isTestContinue(userId, num, isContinue, time);
    }

    @Operation(summary = "上传所有测试结果")
    @GetMapping("uploadAll")
    public Result upLoadAll(@Parameter(description = "测试ID") @RequestParam String testId) {
        return Result.success(usertestService.uploadAll(testId));
    }

    @Operation(summary = "获取用户测试列表", description = "获取指定用户的所有测试记录")
    @GetMapping("getMyTest")
    public Result getMyTest(@Parameter(description = "用户ID") @RequestParam String userId) {
        return Result.success(usertestService.getMyTest(userId));
    }

    @Operation(summary = "获取所有用户测试", description = "分页获取所有用户的测试记录")
    @GetMapping("getUserTest")
    public Result getUserTest(@Parameter(description = "页码") @RequestParam int PageNum,
                               @Parameter(description = "每页数量") @RequestParam int PageSize) {
        return Result.success(usertestService.getUserTest(PageNum, PageSize));
    }

    @Operation(summary = "搜索用户测试", description = "根据关键词搜索测试记录")
    @GetMapping("searchUserTest")
    public Result searchTest(@Parameter(description = "关键词") @RequestParam String keyWord,
                              @Parameter(description = "页码") @RequestParam int PageNum,
                              @Parameter(description = "每页数量") @RequestParam int PageSize) {
        return Result.success(usertestService.searchTest(keyWord, PageNum, PageSize));
    }
}

