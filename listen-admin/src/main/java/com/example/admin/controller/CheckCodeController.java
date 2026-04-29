package com.example.admin.controller;

import com.example.common.common.Result;
import com.example.common.redis.RedisComponent;
import com.google.code.kaptcha.Producer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "验证码接口")
@RestController
public class CheckCodeController {

    @Autowired
    private RedisComponent redisComponent;

    @Autowired
    private Producer captchaProducer;

    @Operation(summary = "获取验证码", description = "获取图形验证码并返回Base64编码的图片")
    @RequestMapping("/checkcode")
    public Result checkcode() throws IOException {
        String code = captchaProducer.createText();
        BufferedImage captchaImage = captchaProducer.createImage(code);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(captchaImage, "png", byteArrayOutputStream);
        byte[] captchaBytes = byteArrayOutputStream.toByteArray();
        String captchaBase64 = Base64.getEncoder().encodeToString(captchaBytes);

        String checkCodeKey = redisComponent.saveCheckCode(code);
        Map<String, String> result = new HashMap<>();
        result.put("checkCode", "data:image/png;base64," + captchaBase64);
        result.put("checkCodeKey", checkCodeKey);
        return Result.success(result);
    }
}
