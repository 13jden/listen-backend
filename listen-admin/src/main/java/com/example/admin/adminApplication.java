package com.example.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = "com.example.admin")
@MapperScan("com.example.wx.mapper")
@ComponentScan(basePackages = {"com.example.admin", "com.example.common","com.example.wx"})
public class adminApplication {
    public static void main(String[] args) {
        SpringApplication.run(adminApplication.class, args);
    }
}
