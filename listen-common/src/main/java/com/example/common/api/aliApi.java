package com.example.common.api;


import org.springframework.context.annotation.Bean;

public class aliApi {

    private static String appKey = "mDqEdf7G7QWRKyZ1";
    private static String accessKeyId = "LTAI5tQn8KAj4dVkpg7uwpgT";
    private static String accessKeySecret = "CUxOfmG0EfnznPRq6mp5St66t8B780";
    private static String gatewayUrl = "wss://nls-gateway.aliyuncs.com/ws/v1";
    public static String getString(String filePath){
        SpeechRecognizerDemo recognizer = new SpeechRecognizerDemo(appKey, accessKeyId, accessKeySecret, gatewayUrl);
        String result = recognizer.recognizeSpeech(filePath, 16000);
        System.out.println("Recognition Result: " + result);
        recognizer.shutdown(); // 记得关闭客户端
        return result;
    }

}
