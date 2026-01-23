package com.example.wx.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.common.common.Result;
import com.example.common.dto.TestDto;
import com.example.common.dto.UserTestDto;
import com.example.wx.pojo.Usertest;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author dzk
 * @since 2025-02-06
 */
@Service
public interface UsertestService extends IService<Usertest> {

    List<TestDto> getTest(String userId, int num);

    Result isTestContinue(String userId, int num,boolean isContinue,int time);

    Usertest uploadAll(String testId);

    List<Usertest> getMyTest(String userId);

    IPage<UserTestDto> getUserTest(int pageNum, int pageSize);

    IPage<UserTestDto> searchTest(String keyWord,int PageNum,int PageSize);
}
