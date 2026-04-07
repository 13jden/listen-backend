package com.example.admin.controller;

import com.example.common.common.ControllerTool;
import com.example.common.common.Result;
import com.example.common.common.WxLoginTool;
import com.example.common.constants.Constants;
import com.example.common.dto.*;
import com.example.common.redis.RedisComponent;
import com.example.wx.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final ControllerTool controllerTool;
    private final RedisComponent redisComponent;
    private final WxLoginTool wxLoginTool;

    @PostMapping("/register")
    public Result register(HttpServletRequest request,
                           HttpServletResponse response,
                           @RequestBody UserRegisterRequest req) {
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(Constants.TOKEN_WX)) {
                    token = cookie.getValue();
                }
            }
        }

        if (token != null) {
            redisComponent.cleanAdminToken(token);
        }

        UserInfoDto userInfoDto = userService.register(
                req.getOpenId(),
                req.getName(),
                req.getHospitalId(),
                req.getNumber(),
                req.getMedicalId(),
                req.getAge()
        );

        controllerTool.saveToken2Cookie(response, token);
        return Result.success(userInfoDto);
    }

    @RequestMapping("list")
    public Result getList(@RequestParam int pageNum,
                          @RequestParam int pageSize) {
        return Result.success(userService.getUserList(pageNum, pageSize));
    }

    @RequestMapping("search")
    public Result search(@RequestParam String word,
                         @RequestParam int pageNum,
                         @RequestParam int pageSize) {
        return Result.success(userService.getUser(word, pageNum, pageSize));
    }

    @RequestMapping("/update")
    public Result updateUser(@RequestBody UserUpdateRequest req) {
        boolean success = userService.updateUser(req.getUserId(), req.getName(), req.getHospitalId(), req.getNumber());
        return success ? Result.success("修改成功") : Result.error("用户不存在");
    }

    @RequestMapping("/delete")
    public Result deleteUser(@NotEmpty String userId) {
        userService.deleteUser(userId);
        return Result.success("删除成功");
    }

    @RequestMapping("login")
    public Result login(@RequestParam @NotEmpty String code) {
        String openId = wxLoginTool.wxLogin(code);
        UserInfoDto userInfoDto = userService.login(openId);
        return Result.success(userInfoDto);
    }
}
