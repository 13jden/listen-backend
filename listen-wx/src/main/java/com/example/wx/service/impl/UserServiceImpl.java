package com.example.wx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.constants.Constants;
import com.example.common.dto.TokenUserInfoDto;
import com.example.common.dto.UserInfoDto;
import com.example.common.exception.UserLoginException;
import com.example.common.utils.CopyTools;
import com.example.common.utils.StringTools;
import com.example.wx.mapper.HospitalMapper;
import com.example.wx.mapper.TestdetailMapper;
import com.example.wx.mapper.UsertestMapper;
import com.example.wx.pojo.Hospital;
import com.example.wx.pojo.User;
import com.example.wx.mapper.UserMapper;
import com.example.wx.pojo.Usertest;
import com.example.wx.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author dzk
 * @since 2025-01-24
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private HospitalMapper hospitalMapper;

    @Autowired
    private UsertestMapper usertestMapper;

    @Autowired
    private TestdetailMapper testdetailMapper;

    @Override
    public UserInfoDto login(String openid) {
        User user = userMapper.findByOpenId(openid);
        if(user==null){
            System.out.println("账号为空");
            throw new UserLoginException("账号不存在："+openid);
        }
        UserInfoDto userInfoDto = CopyTools.copy(user,UserInfoDto.class);
        userInfoDto.setHospital(hospitalMapper.selectById(user.getHospitalId()).getName());
        userInfoDto.setNumber(user.getNumber());

        userInfoDto.setTestTimes(usertestMapper.getTestByUserId(user.getUserId()).size());
        userInfoDto.setRecentTestDate(usertestMapper.getLatestTestDate(user.getUserId()));
        System.out.println(userInfoDto.getRecentTestDate());

        Usertest latestTest = usertestMapper.getLatestTestByUserId(user.getUserId());
        userInfoDto.setScore(latestTest != null ? latestTest.getAvgScore() : 0);
        userInfoDto.setAvgScore(Integer.valueOf(String.valueOf(usertestMapper.getScoreByUserId(user.getUserId()))));

        return userInfoDto;
    }

    @Override
    public TokenUserInfoDto register(String openid, String name,int hospitalId, String number,String medicalId) {
        User user = new User();
        user.setOpenId(openid);
        user.setUserId(StringTools.getRandomBumber(Constants.LENGTH_10));
        user.setHospitalId(hospitalId);
        user.setMedicalId(medicalId);
        user.setName(name);
        user.setNumber(number);
        user.setHospitalId(hospitalId);
        user.setRegisterTime(new Date());
        userMapper.insert(user);
        TokenUserInfoDto tokenUserInfoDto = CopyTools.copy(user,TokenUserInfoDto.class);
        tokenUserInfoDto.setHospital(hospitalMapper.selectById(hospitalId).getName());
        return tokenUserInfoDto;
    }
    @Override
    public IPage getUserList(int pageNum, int pageSize) {
        // 创建分页对象
        Page<User> page = new Page<>(pageNum, pageSize);
        IPage<User> userPage = userMapper.selectPage(page, null);

        return userPage.convert(user -> {
            UserInfoDto userInfo = CopyTools.copy(user,UserInfoDto.class);
            if (user.getHospitalId() != null) {
                Hospital hospital = hospitalMapper.selectById(user.getHospitalId());
                userInfo.setHospital(hospital.getName());
            }
            userInfo.setScore(usertestMapper.getScoreByUserId(user.getUserId()));
            userInfo.setTestTimes(usertestMapper.getTimesByUserId(user.getUserId()));
            return userInfo;
        });
    }

    @Override
    public IPage<UserInfoDto> getUser(String keyword, int pageNum, int pageSize) {
        // 创建分页对象
        Page<User> page = new Page<>(pageNum, pageSize);

        // 创建查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 子查询：根据 hospital.name 查询 hospital_id
            QueryWrapper<Hospital> hospitalQueryWrapper = new QueryWrapper<>();
            hospitalQueryWrapper.select("id").like("name", keyword);
            List<Object> hospitalIds = hospitalMapper.selectObjs(hospitalQueryWrapper);

            // 模糊匹配条件
            queryWrapper.eq("name", keyword)  // 姓m
                    .or()
                    .eq("number", keyword)  // 手机号模糊匹配
                    .or()
                    .eq("medical_id",keyword)//病历号
                    .or()
                    .in(hospitalIds != null && !hospitalIds.isEmpty(), "hospital_id", hospitalIds); // 医院名称模糊匹配
        }

        // 分页查询
        IPage<User> userPage = userMapper.selectPage(page, queryWrapper);

        // 转换为 UserInfoDto
        return userPage.convert(user -> {
            UserInfoDto userInfo = CopyTools.copy(user, UserInfoDto.class);

            if (user.getHospitalId() != null) {
                Hospital hospital = hospitalMapper.selectById(user.getHospitalId());
                if (hospital != null) {
                    userInfo.setHospital(hospital.getName());
                }
            }
            userInfo.setScore(usertestMapper.getScoreByUserId(user.getUserId()));
            userInfo.setTestTimes(usertestMapper.getTimesByUserId(user.getUserId()));
            return userInfo;
        });
    }

    @Override
    public boolean deleteUser(String userId) {
        List<Usertest> userTests = usertestMapper.getTestByUserId(userId);
        for (Usertest usertest : userTests) {
            // 删除文件夹及其内容
            Path folderPath = Paths.get(usertest.getTestFilePath());
            try {
                Files.walk(folderPath)
                        .sorted(Comparator.reverseOrder()) // 先删除子文件和子目录
                        .forEach(path -> {
                            try {
                                if (Files.exists(path)) {  // 检查文件/目录是否存在
                                    Files.delete(path);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        userMapper.deleteById(userId);
        return true; // 返回 true 表示删除成功
    }
}
