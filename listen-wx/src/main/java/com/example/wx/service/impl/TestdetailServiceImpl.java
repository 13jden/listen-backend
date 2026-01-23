package com.example.wx.service.impl;

import com.example.common.api.aliApi;
import com.example.common.common.Result;
import com.example.common.common.getHttpAudio;
import com.example.common.constants.Constants;
import com.example.common.dto.TestDto;
import com.example.common.dto.TokenUserInfoDto;
import com.example.common.dto.UserDetailInfo;
import com.example.common.redis.RedisComponent;
import com.example.common.utils.CopyTools;
import com.example.common.utils.StringTools;
import com.example.wx.mapper.AudioMapper;
import com.example.wx.mapper.UserMapper;
import com.example.wx.mapper.UsertestMapper;
import com.example.wx.pojo.Audio;
import com.example.wx.pojo.Testdetail;
import com.example.wx.mapper.TestdetailMapper;
import com.example.wx.pojo.User;
import com.example.wx.pojo.Usertest;
import com.example.wx.service.TestdetailService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author dzk
 * @since 2025-02-06
 */
@Service
public class TestdetailServiceImpl extends ServiceImpl<TestdetailMapper, Testdetail> implements TestdetailService {

    @Autowired
    private TestdetailMapper testdetailMapper;

    @Autowired
    private UsertestMapper usertestMapper;

    @Autowired
    private AudioMapper audioMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisComponent redisComponent;

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Value("${temp.path}")
    private String TempPath;


    /**
    * 保存音频到usertest.getTestFilePath(),调阿里api语音转文字,把返回的文字存进数据库
    * */
    @Override
    public Result OneUserAudioUpload(MultipartFile testAudio, String testDetailId) throws Exception {
        // 查询测试详情
        Testdetail testdetail = testdetailMapper.selectById(testDetailId);
        if (testdetail == null) {
            throw new RuntimeException("测试详情不存在");
        }
        if (testdetail.getUserAudioPath()!=null){
            //如果上传过相应的文件，则删除文件
            File oldFile = new File(testdetail.getUserAudioPath());
            oldFile.delete();
            System.out.println("删除成功");
        }

        // 查询用户测试信息
        Usertest usertest = usertestMapper.selectById(testdetail.getTestId());

        if (usertest == null) {
            throw new RuntimeException("用户测试信息不存在");
        }

        // 查询音频信息
        Audio audio = audioMapper.selectById(testdetail.getAudioId());
        if (audio == null) {
            throw new RuntimeException("音频信息不存在");
        }

        // 保存音频文件
        String originalFilename = testAudio.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = testdetail.getIndex()+extension; // 序号 + 原拓展名
        String tempName = 100-testdetail.getIndex() + extension;
        String tempPath = usertest.getTestFilePath() + "/" + tempName;
        String fullPath = usertest.getTestFilePath() + "/" + fileName;
        System.out.println("保存路径: " + fullPath);

        File targetFile = new File(tempPath);
        try {
            // 创建目录（如果不存在）
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }
            // 保存文件
            testAudio.transferTo(targetFile);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("文件保存失败");
        }
        System.out.println("保存成功，准备修改格式");
        // 调用FFmpeg修改采样频率
        try {
            modifyAudioSampleRate(tempPath,fullPath);
        } catch (IOException e) {
            return Result.error("修改音频采样频率失败");
        }
        System.out.println("格式修改成功，准备识别语音");
        targetFile.delete();//删除缓存音频；
        // 调用阿里云语音识别 API
        String userContent = aliApi.getString(fullPath); // 上传文件并获取识别结果
        String audioContent = audio.getContent(); // 标准音频文本
        System.out.println("识别文字是："+userContent+"正确的文字是："+audioContent);
        System.out.println("现在开始计算准确率");
        // 计算得分（基于 Levenshtein 距离）
        int score = calculateScore(userContent, audioContent);
        System.out.println("准确率为："+score);
        // 更新测试详情信息
        testdetail.setUserAudioPath(fullPath);
        testdetail.setUserContent(userContent);
        testdetail.setTestTime(new Date());
        testdetail.setScore(score);

        testdetailMapper.updateById(testdetail);
        // 返回 DTO
        TestDto testDto = CopyTools.copy(testdetail, TestDto.class);
        testDto.setAudioPath(getHttpAudio.getAudioUrl(audio.getPath()));
        testDto.setScore(score);
        testDto.setTestAudioPath(getHttpAudio.getAudioUrl(testdetail.getUserAudioPath()));
        return Result.success(testDto);
    }

    @Override
    public Result getTestDetail(String testId) {
//        List<Testdetail> testdetailList = redisComponent.getTestDetail(testId);
//        if(redisComponent.getTestDetail(testId)==null){
        List<Testdetail> testdetailList = testdetailMapper.selectByTestId(testId);
            List<TestDto> testDtoList = new ArrayList<>();
            for(Testdetail testdetail : testdetailList){
                Audio audio = audioMapper.selectById(testdetail.getAudioId());
                if (audio == null) {
                    throw new RuntimeException("音频信息不存在");
                }
                TestDto testDto = CopyTools.copy(testdetail, TestDto.class);
                testDto.setAudioPath(getHttpAudio.getAudioUrl(audio.getPath()));
                testDto.setScore(testdetail.getScore());
                testDto.setTestAudioPath(getHttpAudio.getAudioUrl(testdetail.getUserAudioPath()));
                testDto.setAudioPath(getHttpAudio.getAudioUrl(audio.getPath()));
                testDto.setUserText(testdetail.getUserContent());
                testDto.setTestText(audio.getContent());
                testDto.setTestTime(testdetail.getTestTime());
                System.out.println(testDto.getAudioPath());
                testDtoList.add(testDto);
            }
//            redisComponent.saveTestDetailList(testdetailList);
//        }

        return Result.success(testDtoList);
    }

    @Override
    public Result getPreScore(MultipartFile testAudio) {
        // 保存音频文件
        String originalFilename = testAudio.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String tempName = StringTools.getRandomBumber(5)+extension; // 序号 + 原拓展名
        String tempPath = TempPath + "/" + tempName;
        String tempPath2 = TempPath + "/" + "out_"+tempName;
        System.out.println("保存路径: " + tempPath);
        System.out.println("输出路径: " + tempPath2);
        File targetFile = new File(tempPath);
        try {
            // 创建目录（如果不存在）
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }
            // 保存文件
            testAudio.transferTo(targetFile);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("文件保存失败");
        }
        System.out.println("保存成功，准备修改格式");
        // 调用FFmpeg修改采样频率
        try {
            modifyAudioSampleRate(tempPath,tempPath2);
        } catch (IOException e) {
            return Result.error("修改音频采样频率失败");
        }
        System.out.println("格式修改成功，准备识别语音");
        targetFile.delete();//删除缓存音频；
        // 调用阿里云语音识别 API
        String userContent = aliApi.getString(tempPath2); // 上传文件并获取识别结果
        String audioContent = "旅行团去北京参观故宫";
        System.out.println("识别文字是："+userContent+"正确的文字是："+audioContent);
        System.out.println("现在开始计算准确率");
        // 计算得分（基于 Levenshtein 距离）
        int score = calculateScore(userContent, audioContent);
        System.out.println("准确率为："+score);
        // 更新测试详情信息
        return Result.success(score);
    }

    /**
     * 计算用户语音识别结果与标准文本的相似度得分（基于 Levenshtein 距离）
     *
     * @param userContent   用户语音识别结果
     * @param audioContent  标准音频文本
     * @return 得分（百分比）
     */
    private int calculateScore(String userContent, String audioContent) {
        if (userContent == null || audioContent == null) {
            return 0;
        }
        // 去除非中文字符（保留中文、数字、字母）
        String userText = userContent.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");
        String audioText = audioContent.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");
        System.out.println("用户文本：" + userText);
        System.out.println("正确文本：" + audioText);

        // 计算 Levenshtein 距离
        int distance = levenshteinDistance(userText, audioText);

        // 计算相似度百分比
        int maxLength = Math.max(userText.length(), audioText.length());
        if (maxLength == 0) {
            return 100; // 如果两个字符串都为空，则认为完全匹配
        }
        return (int) ((1 - (double) distance / maxLength) * 100);
    }


    /**
     * 计算两个字符串的 Levenshtein 距离（编辑距离）
     *
     * @param s1 字符串 1
     * @param s2 字符串 2
     * @return Levenshtein 距离
     */
    private int levenshteinDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        int[][] dp = new int[m + 1][n + 1];

        // 初始化动态规划表
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }

        // 填充动态规划表
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1]; // 字符相同，无需操作
                } else {
                    dp[i][j] = Math.min(
                            dp[i - 1][j] + 1,    // 删除操作
                            Math.min(
                                    dp[i][j - 1] + 1,    // 插入操作
                                    dp[i - 1][j - 1] + 1 // 替换操作
                            )
                    );
                }
            }
        }

        return dp[m][n];
    }
    private void modifyAudioSampleRate(String inputFilePath,String outputFilePath) throws IOException {

        // 构造FFmpeg命令，修改采样率为16kHz，16位
        String command = ffmpegPath + " -i " + inputFilePath + " -ar 16000 -ac 1 -sample_fmt s16 -b:a 256k " + outputFilePath;

        // 执行命令
        Process process = Runtime.getRuntime().exec(command);
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("FFmpeg命令执行失败，退出码：" + exitCode);
            }
        } catch (InterruptedException e) {
            throw new IOException("FFmpeg命令执行被中断", e);
        }
    }

    @Override
    public List<UserDetailInfo> getUserDetail(String testId) {
        List<Testdetail> testdetailList = testdetailMapper.selectByTestId(testId);
        if(testdetailList.isEmpty())
            throw new RuntimeException("测试id错误");
        List<UserDetailInfo> userDetailInfoList = new ArrayList<>();
        for(Testdetail t:testdetailList){
            UserDetailInfo userDetailInfo = new UserDetailInfo();
            userDetailInfo.setTestDetailId(t.getId());
            Audio audio = audioMapper.selectById(t.getAudioId());
            userDetailInfo.setIndex(t.getIndex());
            userDetailInfo.setAudioContent(audio.getContent());
            userDetailInfo.setUserContent(t.getUserContent());
            userDetailInfo.setScore(t.getScore());
            userDetailInfo.setTime(t.getTestTime());
            userDetailInfo.setUserAudioPath(getHttpAudio.getAudioUrl(t.getUserAudioPath()));
            userDetailInfo.setAudioPath(getHttpAudio.getAudioUrl(audio.getPath()));
            userDetailInfoList.add(userDetailInfo);
        }

        return userDetailInfoList;
    }

}
