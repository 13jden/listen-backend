package com.example.common.common;

public class getHttpAudio {
    public static String getAudioUrl(String localFilePath) {

        if(localFilePath!=null){
//            String baseUrl = "https://aidatech.cn/";  // Nginx 映射的 HTTP 路径
            String baseUrl = "http://115.190.53.97/";
            String fileName = localFilePath.replace("usr/local/listen/temp", "");  // 提取文件名
            return baseUrl + fileName;
        }
        return null;
    }

}
