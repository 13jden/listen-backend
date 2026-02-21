package com.example.admin.controller;



import com.example.common.common.ControllerTool;
import com.example.common.common.Result;
import com.example.common.constants.Constants;
import com.example.common.dto.AdminLoginDTO;
import com.example.common.dto.IndexDataVO;
import com.example.common.dto.TokenAdminDto;
import com.example.common.redis.RedisComponent;
import com.example.common.utils.StringTools;
import com.example.wx.mapper.UserMapper;
import com.example.wx.mapper.UsertestMapper;
import com.example.wx.pojo.Admin;
import com.example.wx.service.AdminService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Date;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author dzk
 * @since 2025-01-24
 */
@RestController
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private AdminService adminService;

    @Autowired
    private RedisComponent redisComponent;

    @Autowired
    private ControllerTool controllerTool;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UsertestMapper usertestMapper;


    @PostMapping("/login")
    public Result Login(HttpServletRequest request,
                        HttpServletResponse response,
                        @RequestBody AdminLoginDTO loginDTO){
        String name = loginDTO.getName();
        String password = loginDTO.getPassword();
        String checkCodeKey = loginDTO.getCheckCodeKey();
        String checkCode = loginDTO.getCheckCode();

        if(!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))){
            return Result.error("验证码错误");
        }

        redisComponent.cleanCheckCode(checkCodeKey);
        System.out.println(name);
        System.out.println(password);
        TokenAdminDto admin = adminService.login(name,password);
        String token=null;
        //清上一条token
        if(request.getCookies()!=null){
            for(Cookie cookie:request.getCookies()){
                if(cookie.getName().equals(Constants.TOKEN_ADMIN)){
                    token = cookie.getValue();
                }
            }
        }

        if(!StringTools.isEmpty(token)){
            redisComponent.cleanAdminToken(token);
        }
        token = redisComponent.saveAdminTokenInfo(name);
        controllerTool.saveTokenAdminCookie(response,token);
        admin.setToken(token);
        return Result.success(admin);
    }


    @RequestMapping("getIndexdata")
    public Result getIndexdata() {
        IndexDataVO indexData = redisComponent.getDataInfo();
        if(indexData!=null){
            return Result.success(indexData);
        }
        indexData = new IndexDataVO();
        Date today = new Date();
        // 获取今日新增用户数
        int newUserNum = userMapper.getUserNum(today);
        // 获取昨日新增用户数
        int userNumYesterday = userMapper.getUserNumYesterday(today);
        // 计算用户数变化百分比
        int userImprove = calculatePercentageChange(newUserNum, userNumYesterday);
        // 获取用户总数
        int userNum = userMapper.getAllNum();
        // 计算用户总数变化百分比
        int userNumImprove = (int) ((newUserNum / (double) userNum) * 100);

        // 获取今日测试次数
        int newTestTimes = usertestMapper.getTimesByTime(today);
        // 获取昨日测试次数
        int newTimesYesterday = usertestMapper.getTimesYesterday(today);
        // 计算测试次数变化百分比
        int newTimesImprove = calculatePercentageChange(newTestTimes, newTimesYesterday);
        // 获取总测试次数
        int allTestTimes = usertestMapper.getAllTimes();
        // 计算总测试次数变化百分比
        int allTimesImprove = (int) ((newTestTimes / (double) allTestTimes) * 100);
        indexData.setNewUserNum(newUserNum);
        indexData.setUserImprove(userImprove);
        indexData.setUserNum(userNum);
        indexData.setUserNumImprove(userNumImprove);
        indexData.setNewTestTimes(newTestTimes);
        indexData.setNewTimesImprove(newTimesImprove);
        indexData.setAllTestTimes(allTestTimes);
        indexData.setAllTimesImprove(allTimesImprove);
        indexData.setTime(today);
        redisComponent.saveDataInfo(indexData);
        return Result.success(indexData);
    }

    // 计算百分比变化
    private int calculatePercentageChange(int todayValue, int yesterdayValue) {
        if (yesterdayValue == 0) {
            return todayValue == 0 ? 0 : 100; // 如果昨天为0，今天有值则变化100%
        }
        return (int) (((todayValue - yesterdayValue) / (double) yesterdayValue) * 100);
    }


}

