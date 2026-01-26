package com.example.admin.controller;


import com.example.common.common.ControllerTool;
import com.example.common.common.Result;
import com.example.common.common.WxLoginTool;
import com.example.common.constants.Constants;
import com.example.common.dto.TokenUserInfoDto;
import com.example.common.dto.UserInfoDto;
import com.example.common.redis.RedisComponent;
import com.example.common.utils.StringTools;
import com.example.wx.pojo.User;
import com.example.wx.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author dzk
 * @since 2025-01-24
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private ControllerTool controllerTool;

    @Autowired
    private RedisComponent redisComponent;

    @Autowired
    private UserService userService;

    @Autowired
    private WxLoginTool wxLoginTool;

    @PostMapping("/register")
    public Result register(HttpServletRequest request,
                           HttpServletResponse response,
                           @RequestBody User user) { // 使用 @RequestBody 接收 JSON 数据
        String token = null;
        // 清上一条 token
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(Constants.TOKEN_WX)) {
                    token = cookie.getValue();
                }
            }
        }

        if (!StringTools.isEmpty(token)) {
            redisComponent.cleanAdminToken(token);
        }

        // 从 User 对象中提取字段值
        String openId = user.getOpenId();
        String name = user.getName();
        int hospitalId = user.getHospitalId();
        String number = user.getNumber();
        String medicalId = user.getMedicalId();

        // 调用注册逻辑
        TokenUserInfoDto tokenUserInfoDto = userService.register(openId, name, hospitalId, number, medicalId);
        UserInfoDto userInfoDto =  userService.login(openId);
        controllerTool.saveToken2Cookie(response, token);

        return Result.success(userInfoDto);
    }
    @RequestMapping("list")
    public Result getList(@RequestParam int pageNum,
                          @RequestParam int pageSize){
        return Result.success(userService.getUserList(pageNum,pageSize));
    }

    @RequestMapping("search")
    public Result getList(@RequestParam String word,
                          @RequestParam int pageNum,
                          @RequestParam int pageSize){
        return Result.success(userService.getUser(word,pageNum,pageSize));
    }

    @RequestMapping("/update")
    public Result updateUser(@RequestBody User user){
        User user1 = userService.getById(user.getUserId());
        user1.setHospitalId(user.getHospitalId());
        user1.setName(user.getName());
        user1.setNumber(user.getNumber());
        userService.updateById(user1);
        return Result.success("修改成功");
    }

    @RequestMapping("/delete")
    public Result deleteUser(@NotEmpty String userId){
        userService.deleteUser(userId);
        return Result.success("删除成功");
    }

//    @RequestMapping("getHospital")
//    public Result getHospital(){
//        return Result.success(hospitalService.getlist());
//    }

    @RequestMapping("login")
    public Result login(HttpServletResponse response,
                        @RequestParam @NotEmpty String code){
        String openId = wxLoginTool.wxLogin(code);
        System.out.println(openId);
        UserInfoDto userInfoDto =  userService.login(openId);
//        String token = redisComponent.saveTokenInfo(userInfoDto);
//        controllerTool.saveToken2Cookie(response,token);
        return Result.success(userInfoDto);
    }

//    @RequestMapping("/autologin")
//    public Result autologin(HttpServletResponse response) {
//        UserInfoDto tokenUserInfoDto = controllerTool.getTokenUserInfoDto();
//        if(tokenUserInfoDto==null)
//            return Result.error("登录失败");
//
//        if(tokenUserInfoDto.getExpireAt() - System.currentTimeMillis() < Constants.REDIS_KEY_EXPIRES_ONE_DAY){
//            redisComponent.saveTokenInfo(userInfoDto);
//            controllerTool.saveToken2Cookie(response,tokenUserInfoDto.getToken());
//        }
//
//        controllerTool.saveToken2Cookie(response,tokenUserInfoDto.getToken());
//        return Result.success("登录成功");
//    }



}

