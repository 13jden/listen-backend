package com.example.wx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.common.dto.TokenAdminDto;
import com.example.wx.pojo.Admin;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author dzk
 * @since 2025-01-24
 */
@Service
public interface AdminService extends IService<Admin> {

    TokenAdminDto login(String name, String password);

}
