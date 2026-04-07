package com.example.wx.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.dto.IndexDataVO;
import com.example.common.dto.TokenAdminDto;
import com.example.common.exception.BusinessException;
import com.example.common.redis.RedisComponent;
import com.example.wx.mapper.AdminMapper;
import com.example.wx.mapper.UsertestMapper;
import com.example.wx.mapper.UserMapper;
import com.example.wx.pojo.Admin;
import com.example.wx.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

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

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UsertestMapper usertestMapper;

    @Override
    public TokenAdminDto login(String name, String password) {
        Admin admin = adminMapper.selectByName(name);
        TokenAdminDto adminDto = new TokenAdminDto().builder()
                .adminId(admin.getAdminId())
                .adminLevel(admin.getAdminLevel())
                .name(admin.getName())
                .build();
        if (passwordEncoder.matches(password, admin.getPassword())) {
            return adminDto;
        } else {
            throw new BusinessException("登录失败");
        }
    }

    @Override
    public IndexDataVO getIndexData() {
        IndexDataVO cachedData = redisComponent.getDataInfo();
        if (cachedData != null) {
            return cachedData;
        }

        IndexDataVO indexData = new IndexDataVO();
        Date today = new Date();

        int newUserNum = userMapper.getUserNum(today);
        int userNumYesterday = userMapper.getUserNumYesterday(today);
        int userNum = userMapper.getAllNum();

        int newTestTimes = usertestMapper.getTimesByTime(today);
        int newTimesYesterday = usertestMapper.getTimesYesterday(today);
        int allTestTimes = usertestMapper.getAllTimes();

        indexData.setNewUserNum(newUserNum);
        indexData.setUserImprove(calculatePercentageChange(newUserNum, userNumYesterday));
        indexData.setUserNum(userNum);
        indexData.setUserNumImprove(userNum > 0 ? (int) ((newUserNum / (double) userNum) * 100) : 0);
        indexData.setNewTestTimes(newTestTimes);
        indexData.setNewTimesImprove(calculatePercentageChange(newTestTimes, newTimesYesterday));
        indexData.setAllTestTimes(allTestTimes);
        indexData.setAllTimesImprove(allTestTimes > 0 ? (int) ((newTestTimes / (double) allTestTimes) * 100) : 0);
        indexData.setTime(today);

        redisComponent.saveDataInfo(indexData);
        return indexData;
    }

    private int calculatePercentageChange(int todayValue, int yesterdayValue) {
        if (yesterdayValue == 0) {
            return todayValue == 0 ? 0 : 100;
        }
        return (int) (((todayValue - yesterdayValue) / (double) yesterdayValue) * 100);
    }
}
