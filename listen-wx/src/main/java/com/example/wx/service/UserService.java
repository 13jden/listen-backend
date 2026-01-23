package com.example.wx.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.common.dto.TokenUserInfoDto;
import com.example.common.dto.UserInfoDto;
import com.example.wx.pojo.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author dzk
 * @since 2025-01-24
 */
public interface UserService extends IService<User> {

    UserInfoDto login(String openid);

    TokenUserInfoDto register(String openid, String name, int hospitalId, String number,String medicalId);

    //查所有用户
    IPage<UserInfoDto> getUserList(int pageNum, int pageSize);

    //通过手机号/医院名/姓名/模糊查询用户
    IPage<UserInfoDto> getUser(String word,int pageNum,int pageSize);

    boolean deleteUser(String userId);
}
