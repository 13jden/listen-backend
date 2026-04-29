package com.example.admin.controller;

import com.example.common.common.ControllerTool;
import com.example.common.common.Result;
import com.example.common.common.WxLoginTool;
import com.example.common.constants.Constants;
import com.example.common.dto.*;
import com.example.common.redis.RedisComponent;
import com.example.wx.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final ControllerTool controllerTool;
    private final RedisComponent redisComponent;
    private final WxLoginTool wxLoginTool;

    @Operation(summary = "用户注册", description = "注册新用户")
    @PostMapping("/register")
    public Result register(HttpServletRequest request,
                           HttpServletResponse response,
                           @Parameter(description = "注册信息") @RequestBody UserRegisterRequest req) {
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

    @Operation(summary = "获取用户列表", description = "分页获取用户列表")
    @GetMapping("/list")
    public Result getList(@Parameter(description = "页码") @RequestParam int pageNum,
                          @Parameter(description = "每页数量") @RequestParam int pageSize) {
        return Result.success(userService.getUserList(pageNum, pageSize));
    }

    @Operation(summary = "搜索用户", description = "根据关键词搜索用户")
    @GetMapping("/search")
    public Result search(@Parameter(description = "搜索关键词") @RequestParam String word,
                         @Parameter(description = "页码") @RequestParam int pageNum,
                         @Parameter(description = "每页数量") @RequestParam int pageSize) {
        return Result.success(userService.getUser(word, pageNum, pageSize));
    }

    @Operation(summary = "更新用户信息")
    @PostMapping("/update")
    public Result updateUser(@Parameter(description = "更新信息") @RequestBody UserUpdateRequest req) {
        boolean success = userService.updateUser(req.getUserId(), req.getName(), req.getHospitalId(), req.getNumber());
        return success ? Result.success("修改成功") : Result.error("用户不存在");
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/delete")
    public Result deleteUser(@Parameter(description = "用户ID") @NotEmpty String userId) {
        userService.deleteUser(userId);
        return Result.success("删除成功");
    }

    @Operation(summary = "用户登录", description = "微信授权登录")
    @GetMapping("/login")
    public Result login(@Parameter(description = "微信授权码") @RequestParam @NotEmpty String code) {
        String openId = wxLoginTool.wxLogin(code);
        UserInfoDto userInfoDto = userService.login(openId);
        return Result.success(userInfoDto);
    }
}
