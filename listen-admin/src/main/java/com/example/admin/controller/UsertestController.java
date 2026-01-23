package com.example.admin.controller;


import com.example.common.common.Result;
import com.example.wx.service.AudioService;
import com.example.wx.service.UsertestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author dzk
 * @since 2025-02-06
 */
@RestController
@RequestMapping("/usertest")
public class UsertestController {

    @Autowired
    private AudioService audioService;

    @Autowired
    private UsertestService usertestService;

    /**
     * 创建（用户id+时间戳）一个文件夹，创建33条用户详细数据，测试部分为空，用户每保存一个数据，添加一次；
     */
    @RequestMapping("getTest")
    public Result getTest(@RequestParam String userId,@RequestParam int num,@RequestParam boolean isContinue,@RequestParam int time){
        return usertestService.isTestContinue(userId,num,isContinue,time);
    }

    @RequestMapping("uploadAll")
    public Result upLoadAll(@RequestParam String testId){
        return Result.success(usertestService.uploadAll(testId));
    }

    @RequestMapping("getMyTest")
    public Result getMyTest(@RequestParam String userId){
        return Result.success(usertestService.getMyTest(userId));
    }



    @RequestMapping("getUserTest")
    public Result getUserTest(@RequestParam int PageNum,@RequestParam int PageSize){
        return Result.success(usertestService.getUserTest(PageNum,PageSize));
    }

    @RequestMapping("searchUserTest")
    public Result searchTest(@RequestParam String keyWord , int PageNum ,int PageSize){
        return Result.success(usertestService.searchTest(keyWord , PageNum ,PageSize));
    }
}

