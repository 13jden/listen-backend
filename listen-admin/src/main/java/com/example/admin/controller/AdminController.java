package com.example.admin.controller;

import com.example.common.common.ControllerTool;
import com.example.common.common.Result;
import com.example.common.constants.Constants;
import com.example.common.dto.AdminLoginDTO;
import com.example.common.dto.IndexDataVO;
import com.example.common.dto.TokenAdminDto;
import com.example.common.redis.RedisComponent;
import com.example.wx.service.AdminService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    private final RedisComponent redisComponent;
    private final ControllerTool controllerTool;

    @PostMapping("/login")
    public Result login(HttpServletRequest request,
                        HttpServletResponse response,
                        @RequestBody AdminLoginDTO loginDTO) {
        String checkCode = loginDTO.getCheckCode();
        String checkCodeKey = loginDTO.getCheckCodeKey();

        if (!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))) {
            return Result.error("验证码错误");
        }
        redisComponent.cleanCheckCode(checkCodeKey);

        TokenAdminDto admin = adminService.login(loginDTO.getName(), loginDTO.getPassword());

        String oldToken = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(Constants.TOKEN_ADMIN)) {
                    oldToken = cookie.getValue();
                }
            }
        }
        if (oldToken != null) {
            redisComponent.cleanAdminToken(oldToken);
        }

        String token = redisComponent.saveAdminTokenInfo(loginDTO.getName());
        controllerTool.saveTokenAdminCookie(response, token);
        admin.setToken(token);
        return Result.success(admin);
    }

    @RequestMapping("getIndexdata")
    public Result getIndexdata() {
        IndexDataVO indexData = adminService.getIndexData();
        return Result.success(indexData);
    }
}
