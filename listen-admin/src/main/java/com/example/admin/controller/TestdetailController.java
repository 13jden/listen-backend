package com.example.admin.controller;


import com.example.common.common.Result;
import com.example.common.dto.UserDetailInfo;
import com.example.common.redis.RedisComponent;
import com.example.wx.pojo.Testdetail;
import com.example.wx.pojo.Usertest;
import com.example.wx.service.AudioService;
import com.example.wx.service.TestdetailService;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author dzk
 * @since 2025-02-06
 */
@RestController
@RequestMapping("/testdetail")
public class TestdetailController {

    @Autowired
    private TestdetailService testdetailService;

    @Autowired
    private RedisComponent redisComponent;

    @RequestMapping("OneUserAudio")
    public Result OneUserAudioUpload(@NotNull MultipartFile testAudio, @NotEmpty String testDetailId) throws Exception {
        //存数据库
        return testdetailService.OneUserAudioUpload(testAudio,testDetailId);
    }

    @RequestMapping("getDetail")
    public Result getTestDetail(@NotEmpty String testId) {
        //存数据库
        return testdetailService.getTestDetail(testId);
    }

    @RequestMapping("getPreScore")
    public Result getPreScore(@NotNull MultipartFile testAudio){

        return testdetailService.getPreScore(testAudio);
    }

    @RequestMapping("admin/getDetail")
    public Result getUserDetail(@RequestParam String testId){
        List<UserDetailInfo> userDetailInfoList = redisComponent.getTestInfo(testId);
        if(userDetailInfoList==null){
            userDetailInfoList = testdetailService.getUserDetail(testId);
            redisComponent.saveTestInfo(testId,userDetailInfoList);
            System.out.println("redis获取成功");
        }
        return Result.success(userDetailInfoList);
    }



}

