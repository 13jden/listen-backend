package com.example.common.common;

import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class KaptchaConfig {

    @Bean
    public Producer captchaProducer() {
        Properties properties = new Properties();

        // 图片尺寸
        properties.put("kaptcha.image.width", "150");  // 图片宽度
        properties.put("kaptcha.image.height", "50");  // 图片高度

        // 字符设置
        properties.put("kaptcha.textproducer.font.size", "40");  // 字体大小
        properties.put("kaptcha.textproducer.font.color", "blue");  // 字体颜色
        properties.put("kaptcha.textproducer.char.space", "5");  // 字符间距
        properties.put("kaptcha.textproducer.char.length", "4");  // 验证码字符长度

        // 背景设置
        properties.put("kaptcha.background.clear.from", "white");  // 背景渐变起始颜色
        properties.put("kaptcha.background.clear.to", "lightGray");  // 背景渐变结束颜色

        // 噪声和干扰
        properties.put("kaptcha.noise.impl", "com.google.code.kaptcha.impl.DefaultNoise");  // 噪声干扰
        properties.put("kaptcha.obscurificator.impl", "com.google.code.kaptcha.impl.WaterRipple");  // 水波纹效果

        // 字体设置
        properties.put("kaptcha.textproducer.font.names", "Arial, Courier");  // 字体名称

        // 配置验证码生成器
        Config config = new Config(properties);
        DefaultKaptcha kaptcha = new DefaultKaptcha();
        kaptcha.setConfig(config);
        return kaptcha;
    }

}
