package com.example.wx.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.dto.TokenAdminDto;
import com.example.common.exception.BusinessException;
import com.example.common.redis.RedisComponent;
import com.example.wx.mapper.AdminMapper;
import com.example.wx.pojo.Admin;
import com.example.wx.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Security;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author dzk
 * @since 2025-01-24
 */
@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {
    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private RedisComponent redisComponent;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public TokenAdminDto login(String name, String password) {
        Admin admin = adminMapper.selectByName(name);
        TokenAdminDto adminDto = new TokenAdminDto().builder()
                .adminId(admin.getAdminId())
                .adminLevel(admin.getAdminLevel())
                .name(admin.getName())
                .build();
        if(passwordEncoder.matches(password,admin.getPassword())){
            return adminDto;
        }else {
            throw new BusinessException("登录失败");
        }
    }

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println(encoder.encode("123456"));
    }
}
