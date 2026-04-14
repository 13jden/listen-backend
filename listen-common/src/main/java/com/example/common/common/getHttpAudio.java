package com.example.common.common;

public class getHttpAudio {
    public static String getAudioUrl(String localFilePath) {

        if(localFilePath!=null){
//            String baseUrl = "https://audiotest.top/";  // Nginx 映射的 HTTP 路径
            String baseUrl = "http://115.190.53.97";
            return baseUrl + localFilePath;
        }
        return null;
    }

    public static String getLocalPath(String localFilePath) {
        if(localFilePath!=null){
            // 提取 userFile 之后的相对路径
            // 例如: C:/Users/86182/Desktop/work/listen/userFile/ZJZ100861776134586477/2.wav -> /userFile/ZJZ100861776134586477/2.wav
            String normalizedPath = localFilePath.replace("\\", "/");
            int userFileIndex = normalizedPath.lastIndexOf("userFile/");
            if (userFileIndex != -1) {
                return "http://localhost:8081/" + normalizedPath.substring(userFileIndex);
            }
            // 如果找不到 userFile/，直接返回路径部分
            return "http://localhost:8081/userFile/" + normalizedPath;
        }
        return null;
    }

}
