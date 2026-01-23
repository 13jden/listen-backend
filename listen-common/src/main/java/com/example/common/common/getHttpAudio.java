package com.example.common.common;

public class getHttpAudio {
    public static String getAudioUrl(String localFilePath) {
        // 假设本地路径是 /usr/listenTestFile/audio/example.mp3
        if(localFilePath!=null){
            String baseUrl = "https://aidatech.cn/";  // Nginx 映射的 HTTP 路径
            String fileName = localFilePath.replace("/usr/listenTestFile/", "");  // 提取文件名
            return baseUrl + fileName;
        }
        return null;
    }

}
