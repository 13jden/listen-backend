package com.example.common.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.common.config.AppConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class WxLoginTool {

    // 微信登录接口地址
    private static final String WX_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";

    // HTTP 客户端
    private final OkHttpClient httpClient = new OkHttpClient();

    private final AppConfig appConfig;

    public WxLoginTool(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * 处理微信登录请求
     *
     * @param code 小程序端传来的 code
     * @return 返回 openid
     */
    public String wxLogin(String code) {
        String appId = appConfig.getWxAppId();
        String appSecret = appConfig.getWxAppSecret();

        // 构造请求 URL
        String url = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                WX_LOGIN_URL, appId, appSecret, code);

        // 发送 HTTP 请求
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                // 获取微信服务器的响应
                String responseBody = response.body().string();
                System.out.println("微信服务器响应: " + responseBody);

                // 解析 JSON 响应
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

                // 提取 openid
                String openid = (String) responseMap.get("openid");
                if (openid != null) {
                    return openid; // 返回 openid
                } else {
                    return "登录失败请稍后再试";
                }
            } else {
                System.out.println("请求微信服务器失败，状态码: " + response.code());
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("请求微信服务器时发生异常: " + e.getMessage());
            return null;
        }
    }
}
