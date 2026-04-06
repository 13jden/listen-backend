package com.example.common.common;

public class getHttpAudio {
    public static String getAudioUrl(String localFilePath) {

        if(localFilePath!=null){
//            String baseUrl = "https://audiotest.top/";  // Nginx 映射的 HTTP 路径
            String baseUrl = "http://115.190.53.97/";
            return baseUrl + localFilePath;
        }
        return null;
    }

}
