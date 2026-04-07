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

    UserInfoDto register(String openid, String name, int hospitalId, String number, String medicalId, Integer age);

    IPage<UserInfoDto> getUserList(int pageNum, int pageSize);

    IPage<UserInfoDto> getUser(String word, int pageNum, int pageSize);

    boolean deleteUser(String userId);

    boolean updateUser(String userId, String name, Integer hospitalId, String number);
}
