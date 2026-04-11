package com.example.wx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.constants.Constants;
import com.example.common.dto.UserInfoDto;
import com.example.common.redis.RedisComponent;
import com.example.common.exception.UserLoginException;
import com.example.common.utils.CopyTools;
import com.example.common.utils.StringTools;
import com.example.wx.mapper.HospitalMapper;
import com.example.wx.mapper.UsertestMapper;
import com.example.wx.pojo.Hospital;
import com.example.wx.pojo.User;
import com.example.wx.mapper.UserMapper;
import com.example.wx.pojo.Usertest;
import com.example.wx.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final UserMapper userMapper;
    private final HospitalMapper hospitalMapper;
    private final UsertestMapper usertestMapper;
    private final RedisComponent redisComponent;

    @Override
    public UserInfoDto login(String openid) {
        User user = userMapper.findByOpenId(openid);
        if (user == null) {
            throw new UserLoginException("账号不存在：" + openid);
        }
        return buildUserInfoDto(user);
    }

    @Override
    public UserInfoDto register(String openid, String name, int hospitalId, String number, String medicalId, Integer age) {
        if (StringTools.isEmpty(openid) || openid.length() > 100 || openid.getBytes().length != openid.length()) {
            throw new UserLoginException("openid 无效");
        }

        User existUser = userMapper.findByOpenId(openid);
        if (existUser != null) {
            throw new UserLoginException("用户已存在");
        }

        User user = new User();
        user.setOpenId(openid);
        user.setUserId(StringTools.getRandomBumber(Constants.LENGTH_10));
        user.setHospitalId(hospitalId);
        user.setMedicalId(medicalId);
        user.setName(name);
        user.setNumber(number);
        user.setAge(age);
        user.setRegisterTime(new Date());
        userMapper.insert(user);
        redisComponent.deleteIndexDataCache();

        return buildUserInfoDto(user);
    }

    @Override
    public IPage<UserInfoDto> getUserList(int pageNum, int pageSize) {
        Page<User> page = new Page<>(pageNum, pageSize);
        IPage<User> userPage = userMapper.selectPage(page, null);
        return userPage.convert(this::buildUserInfoDto);
    }

    @Override
    public IPage<UserInfoDto> getUser(String keyword, int pageNum, int pageSize) {
        Page<User> page = new Page<>(pageNum, pageSize);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            QueryWrapper<Hospital> hospitalQueryWrapper = new QueryWrapper<>();
            hospitalQueryWrapper.select("id").like("name", keyword);
            List<Object> hospitalIds = hospitalMapper.selectObjs(hospitalQueryWrapper);

            queryWrapper.eq("name", keyword)
                    .or()
                    .eq("number", keyword)
                    .or()
                    .eq("medical_id", keyword)
                    .or()
                    .in(hospitalIds != null && !hospitalIds.isEmpty(), "hospital_id", hospitalIds);
        }

        IPage<User> userPage = userMapper.selectPage(page, queryWrapper);
        return userPage.convert(this::buildUserInfoDto);
    }

    @Override
    public boolean deleteUser(String userId) {
        List<Usertest> userTests = usertestMapper.getTestByUserId(userId);
        for (Usertest usertest : userTests) {
            Path folderPath = Paths.get(usertest.getTestFilePath());
            try {
                Files.walk(folderPath)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                if (Files.exists(path)) {
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
        redisComponent.deleteIndexDataCache();
        return true;
    }

    @Override
    public boolean updateUser(String userId, String name, Integer hospitalId, String number) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        if (name != null) {
            user.setName(name);
        }
        if (hospitalId != null) {
            user.setHospitalId(hospitalId);
        }
        if (number != null) {
            user.setNumber(number);
        }
        userMapper.updateById(user);
        redisComponent.deleteIndexDataCache();
        return true;
    }

    private UserInfoDto buildUserInfoDto(User user) {
        UserInfoDto userInfoDto = CopyTools.copy(user, UserInfoDto.class);

        if (user.getHospitalId() != null) {
            Hospital hospital = hospitalMapper.selectById(user.getHospitalId());
            if (hospital != null) {
                userInfoDto.setHospital(hospital.getName());
            }
        }

        userInfoDto.setScore(usertestMapper.getScoreByUserId(user.getUserId()));
        userInfoDto.setTestTimes(usertestMapper.getTimesByUserId(user.getUserId()));
        userInfoDto.setRecentTestDate(usertestMapper.getLatestTestDate(user.getUserId()));

        Double avgScore = usertestMapper.getScoreByUserId(user.getUserId());
        userInfoDto.setAvgScore(avgScore != null ? avgScore.intValue() : 0);

        return userInfoDto;
    }
}
