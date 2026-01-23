package com.example.wx.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.StringTools;
import com.example.wx.mapper.AdminMapper;
import com.example.wx.pojo.Admin;
import com.example.wx.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Override
    public Admin login(String name, String password) {
        Admin admin = adminMapper.selectByName(name);
        password = StringTools.encodeByMd5(password);
        System.out.println(admin.getPassword());
        if(admin.getPassword().equals(password))
            return admin;
        else
            return null;
    }
}
