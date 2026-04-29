package com.example.admin.controller;


import com.example.common.common.Result;
import com.example.common.dto.UserDetailInfo;
import com.example.common.redis.RedisComponent;
import com.example.wx.pojo.Testdetail;
import com.example.wx.pojo.Usertest;
import com.example.wx.service.AudioService;
import com.example.wx.service.TestdetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "测试详情接口")
@RestController
@RequestMapping("/testdetail")
public class TestdetailController {

    @Autowired
    private TestdetailService testdetailService;

    @Autowired
    private RedisComponent redisComponent;

    @Operation(summary = "用户上传音频", description = "用户上传测试音频并保存")
    @PostMapping("OneUserAudio")
    public Result OneUserAudioUpload(@Parameter(description = "音频文件") @NotNull MultipartFile testAudio,
                                    @Parameter(description = "测试详情ID") @NotEmpty String testDetailId) throws Exception {
        return testdetailService.OneUserAudioUpload(testAudio, testDetailId);
    }

    @Operation(summary = "获取测试详情", description = "根据测试ID获取测试详情列表")
    @GetMapping("getDetail")
    public Result getTestDetail(@Parameter(description = "测试ID") @NotEmpty String testId) {
        return testdetailService.getTestDetail(testId);
    }

    @Operation(summary = "获取预测分数", description = "上传音频获取AI预测分数")
    @PostMapping("getPreScore")
    public Result getPreScore(@Parameter(description = "音频文件") @NotNull MultipartFile testAudio) {
        return testdetailService.getPreScore(testAudio);
    }

    @Operation(summary = "获取用户测试详情", description = "管理员获取用户的测试详情（优先从Redis获取）")
    @GetMapping("admin/getDetail")
    public Result getUserDetail(@Parameter(description = "测试ID") @RequestParam String testId) {
        List<UserDetailInfo> userDetailInfoList = redisComponent.getTestInfo(testId);
        if (userDetailInfoList == null) {
            userDetailInfoList = testdetailService.getUserDetail(testId);
            redisComponent.saveTestInfo(testId, userDetailInfoList);
            System.out.println("redis获取成功");
        }
        return Result.success(userDetailInfoList);
    }

    @Operation(summary = "获取题目详情", description = "根据详情ID获取单条题目详情")
    @GetMapping("getItemDetail")
    public Result getItemDetail(@Parameter(description = "详情ID") @RequestParam String detailId) {
        return Result.success(testdetailService.getItemDetail(detailId));
    }

}

