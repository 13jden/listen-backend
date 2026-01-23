//package com.example.admin.controller;
//
//import com.example.common.api.aliApi;
//import com.example.common.common.Result;
//import com.example.common.constants.Constants;
//import com.example.common.utils.StringTools;
//import jakarta.validation.constraints.NotEmpty;
//import jakarta.validation.constraints.NotNull;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.File;
//import java.io.IOException;
//
//@RestController
//public class UploadController {
//
//    @Value("${temp.path}")
//    private String tempPath;
//
//    // FFmpeg路径
//    @Value("${ffmpeg.path}")
//    private String ffmpegPath;
//
//
//    @RequestMapping ("/uploadAudio")
//    public Result getAudioString(@NotNull MultipartFile testAudio) throws Exception {
//        if (testAudio.isEmpty()) {
//            return Result.error("上传文件为空");
//        }
//
//        // 生成随机文件名
//        String originalFilename = testAudio.getOriginalFilename();
//        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
//        String fileName = StringTools.getRandomBumber(Constants.LENGTH_10) + extension;
//        String fullPath = tempPath + "/" + fileName;
//
//        // 保存文件到临时路径
//        File targetFile = new File(fullPath);
//        try {
//            testAudio.transferTo(targetFile);
//        } catch (IOException e) {
//            return Result.error("文件保存失败");
//        }
//
//        // 调用FFmpeg修改采样频率
//        try {
//            modifyAudioSampleRate(fullPath, fullPath);
//        } catch (IOException e) {
//            return Result.error("修改音频采样频率失败");
//        }
//
//        // 调用语音识别接口
//        String recognitionResult = aliApi.getString(fullPath);
//
//        if (recognitionResult == null || recognitionResult.isEmpty()) {
//            return Result.error("语音识别失败");
//        }
//
//        // 返回识别结果
//        return Result.success(recognitionResult);
//    }
//
//    private void modifyAudioSampleRate(String inputFilePath, String outputFilePath) throws IOException {
//        // 构造FFmpeg命令，修改采样率为16kHz，16位
//        String command = ffmpegPath + " -i " + inputFilePath + " -ar 16000 -ac 1 -sample_fmt s16 -b:a 256k " + outputFilePath;
//
//        // 执行命令
//        Process process = Runtime.getRuntime().exec(command);
//        try {
//            int exitCode = process.waitFor();
//            if (exitCode != 0) {
//                throw new IOException("FFmpeg命令执行失败，退出码：" + exitCode);
//            }
//        } catch (InterruptedException e) {
//            throw new IOException("FFmpeg命令执行被中断", e);
//        }
//    }
//}
