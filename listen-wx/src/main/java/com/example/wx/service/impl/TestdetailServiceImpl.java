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
import com.example.wx.elasticsearch.service.ElasticsearchSyncService;
import com.example.wx.elasticsearch.service.AIEvaluationService;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
    private ElasticsearchSyncService elasticsearchSyncService;

    @Autowired
    private RedisComponent redisComponent;

    @Autowired
    private AIEvaluationService aiEvaluationService;

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Value("${temp.path}")
    private String TempPath;

    /**
     * 获取音频时长（秒）
     */
    private float getAudioDuration(String filePath) throws IOException {
        String command = ffmpegPath + " -i " + filePath + " 2>&1";
        Process process = Runtime.getRuntime().exec(command);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String line;
        float duration = 0;
        try {
            while ((line = reader.readLine()) != null) {
                if (line.contains("Duration:")) {
                    // 解析 Duration: 00:00:02.50 格式
                    String durationStr = line.substring(line.indexOf("Duration:") + 9, line.indexOf("Duration:") + 21);
                    String[] parts = durationStr.split(":");
                    if (parts.length == 3) {
                        int hours = Integer.parseInt(parts[0]);
                        int minutes = Integer.parseInt(parts[1]);
                        float seconds = Float.parseFloat(parts[2]);
                        duration = hours * 3600 + minutes * 60 + seconds;
                    }
                    break;
                }
            }
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            reader.close();
        }
        return duration;
    }

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

        // 获取用户语音时长
        float userDuration = getAudioDuration(fullPath);
        System.out.println("用户语音时长: " + userDuration + "秒");

        // 获取标准音频时长
        float standardDuration = audio.getDurationSec() != null ? audio.getDurationSec() : 0f;
        System.out.println("标准音频时长: " + standardDuration + "秒");

        System.out.println("现在开始计算得分");

        // 统一调用AI服务获取评分、错误标签和结果分析
        AIEvaluationService.AIEvaluationResult aiResult = null;
        try {
            aiResult = aiEvaluationService.evaluate(userContent, audioContent);
        } catch (Exception e) {
            log.error("AI评分调用失败: {}"+e.getMessage());
        }

        // 计算各部分得分
        float durationScore = calculateDurationScore(userDuration, standardDuration);
        float editDistanceScore = calculateEditDistanceScore(userContent, audioContent);

        // 使用AI评分（如果有），否则使用编辑距离评分
        float aiScore;
        if (aiResult != null && aiResult.getScore() > 0) {
            aiScore = aiResult.getScore();
        } else {
            aiScore = editDistanceScore;
        }

        // 计算最终得分：时长0.2 + 编辑距离0.5 + AI评分0.3
        float score = durationScore * 0.2f + editDistanceScore * 0.5f + aiScore * 0.3f;
        System.out.println("最终得分为：" + score);

        // 获取错误标签和错误详情
        String errorPositions = "";
        String errorTags = "";

        // 去除非中文字符进行位置分析
        String userText = userContent.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");
        String audioText = audioContent.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");
        float similarity = calculateEditDistanceScore(userContent, audioContent);

        if (similarity < 100f) {
            // 计算错误位置
            errorPositions = calculateErrorPositions(userText, audioText);

            // 使用AI返回的错误标签
            if (aiResult != null && aiResult.getErrorTags() != null && !aiResult.getErrorTags().isEmpty()) {
                errorTags = aiResult.getErrorTags();
            } else {
                // 使用本地分析
                errorTags = analyzeErrorTagsLocal(userText, audioText);
            }
        }

        // 生成结果分析（优先使用AI返回的建议）
        String resultAnalysis;
        if (aiResult != null && aiResult.getResultAnalysis() != null && !aiResult.getResultAnalysis().isEmpty()) {
            resultAnalysis = aiResult.getResultAnalysis();
        } else {
            resultAnalysis = generateResultAnalysis(score, errorTags, 1);
        }

        // 更新测试详情信息
        testdetail.setUserAudioPath(fullPath);
        testdetail.setUserContent(userContent);
        testdetail.setTestTime(new Date());
        testdetail.setScore(score);
        testdetail.setErrorPositions(errorPositions);
        testdetail.setErrorTags(errorTags);
        testdetail.setResultAnalysis(resultAnalysis);
        testdetail.setSpeechDurationSec(userDuration);
        testdetail.setStandardDurationSec(standardDuration);

        testdetail.setDurationScore(durationScore);
        testdetail.setEditDistanceScore(editDistanceScore);
        testdetail.setAiScore(aiScore);

        testdetailMapper.updateById(testdetail);
        // 返回 DTO
        TestDto testDto = CopyTools.copy(testdetail, TestDto.class);
        testDto.setAudioPath(getHttpAudio.getAudioUrl(audio.getPath()));
        testDto.setScore(score);
        testDto.setTestAudioPath(getHttpAudio.getAudioUrl(testdetail.getUserAudioPath()));
        testDto.setErrorPositions(errorPositions);
        testDto.setErrorTags(errorTags);
        testDto.setResultAnalysis(resultAnalysis);

        // 同步单条测试详情到ES
        elasticsearchSyncService.syncTestItemToEs(testDto);

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
        float score = calculateEditDistanceScore(userContent, audioContent);
        System.out.println("准确率为："+score);
        // 更新测试详情信息
        return Result.success(score);
    }

    /**
     * 计算最终得分
     * 评分权重：时长得分0.2 + 编辑距离得分0.5 + AI评分0.3
     *
     * @param userContent   用户语音识别结果
     * @param audioContent  标准音频文本
     * @param userDuration  用户语音时长（秒）
     * @param standardDuration 标准音频时长（秒）
     * @return 最终得分
     */
    private float calculateFinalScore(String userContent, String audioContent, float userDuration, float standardDuration) {
        // 1. 时长得分（0.2权重）
        float durationScore = calculateDurationScore(userDuration, standardDuration);

        // 2. 编辑距离得分（0.5权重）
        float editDistanceScore = calculateEditDistanceScore(userContent, audioContent);

        // 3. AI评分（0.3权重）- 预留，先用编辑距离得分代替
        float aiScore = calculateAiScore(userContent, audioContent);

        // 计算最终得分
        float finalScore = durationScore * 0.2f + editDistanceScore * 0.5f + aiScore * 0.3f;

        System.out.println(String.format("得分明细 - 时长得分: %.2f, 编辑距离得分: %.2f, AI得分: %.2f, 最终得分: %.2f",
                durationScore, editDistanceScore, aiScore, finalScore));

        return finalScore;
    }

    /**
     * 计算时长得分
     * 基于用户时长与标准时长的比例，接近标准时长得分越高
     *
     * @param userDuration  用户语音时长（秒）
     * @param standardDuration 标准音频时长（秒）
     * @return 时长得分（0-100）
     */
    private float calculateDurationScore(float userDuration, float standardDuration) {
        if (standardDuration <= 0) {
            return 100f; // 如果没有标准时长，默认满分
        }

        // 计算时长比例
        float ratio = userDuration / standardDuration;

        // 理想比例是1.0，偏离越多扣分越多
        // 比例在0.8-1.2之间为最佳，得100分
        // 超出这个范围逐渐扣分
        float score;
        if (ratio >= 0.8f && ratio <= 1.2f) {
            score = 100f;
        } else if (ratio < 0.8f) {
            // 时长太短
            score = 100f * (userDuration / (standardDuration * 0.8f));
        } else {
            // 时长太长
            score = 100f * (1f - (ratio - 1.2f) / 1.2f);
        }

        return Math.max(0f, Math.min(100f, score));
    }

    /**
     * 计算编辑距离得分（基于 Levenshtein 距离）
     *
     * @param userContent   用户语音识别结果
     * @param audioContent  标准音频文本
     * @return 编辑距离得分（0-100）
     */
    private float calculateEditDistanceScore(String userContent, String audioContent) {
        if (userContent == null || audioContent == null) {
            return 0;
        }

        // 去除非中文字符
        String userText = userContent.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");
        String audioText = audioContent.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");

        // 计算 Levenshtein 距离
        int distance = levenshteinDistance(userText, audioText);

        // 计算相似度百分比
        int maxLength = Math.max(userText.length(), audioText.length());
        if (maxLength == 0) {
            return 100f;
        }

        return (float) ((1 - (double) distance / maxLength) * 100);
    }

    /**
     * 计算AI评分
     * 优先使用编辑距离评分（因为AI服务已经在OneUserAudioUpload中统一调用了）
     *
     * @param userContent   用户语音识别结果
     * @param audioContent  标准音频文本
     * @return AI评分（0-100）
     */
    private float calculateAiScore(String userContent, String audioContent) {
        // AI服务已经在OneUserAudioUpload中调用过了，这里直接使用编辑距离评分作为兜底
        return calculateEditDistanceScore(userContent, audioContent);
    }

    /**
     * AI分析错误位置和错误标签
     * 调用通义千问App进行分析
     *
     * @param userContent   用户语音识别结果
     * @param audioContent  标准音频文本
     * @return 包含错误位置和错误标签的数组 [errorPositions, errorTags]
     */
    private String[] analyzeErrorsByAi(String userContent, String audioContent) {
        String[] result = new String[]{"", ""};

        if (userContent == null || audioContent == null || userContent.isEmpty() || audioContent.isEmpty()) {
            return result;
        }

        // 去除非中文字符进行位置分析
        String userText = userContent.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");
        String audioText = audioContent.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");

        // 计算编辑距离相似度
        float similarity = calculateEditDistanceScore(userContent, audioContent);

        // 如果相似度100%，说明完全正确，无需分析错误
        if (similarity >= 100f) {
            return result;
        }

        // 尝试调用AI进行分析
        try {
            AIEvaluationService.AIEvaluationResult aiResult = aiEvaluationService.evaluate(userContent, audioContent);

            // 设置错误标签（从AI结果获取）
            if (aiResult.getErrorTags() != null && !aiResult.getErrorTags().isEmpty()) {
                result[1] = aiResult.getErrorTags();
            }

            // 设置错误位置（本地计算）
            result[0] = calculateErrorPositions(userText, audioText);

            // 如果AI没有返回错误标签，使用本地分析
            if (result[1].isEmpty()) {
                result[1] = analyzeErrorTagsLocal(userText, audioText);
            }

            // 将AI返回的error详情保存到全局resultAnalysis中
            // 注意：这个值会在OneUserAudioUpload方法中设置到testdetail的resultAnalysis字段

        } catch (Exception e) {
            log.error("AI错误分析调用失败，使用本地分析作为兜底: {}"+e.getMessage());
            // 使用本地分析作为兜底
            return analyzeErrors(userContent, audioContent);
        }

        return result;
    }

    /**
     * 计算错误位置
     */
    private String calculateErrorPositions(String userText, String audioText) {
        List<Integer> errorPositions = new ArrayList<>();

        int minLen = Math.min(userText.length(), audioText.length());
        for (int i = 0; i < minLen; i++) {
            if (userText.charAt(i) != audioText.charAt(i)) {
                errorPositions.add(i + 1);
            }
        }

        if (userText.length() > audioText.length()) {
            for (int i = audioText.length(); i < userText.length(); i++) {
                errorPositions.add(i + 1);
            }
        } else if (audioText.length() > userText.length()) {
            for (int i = userText.length(); i < audioText.length(); i++) {
                errorPositions.add(i + 1);
            }
        }

        if (errorPositions.isEmpty()) {
            return "";
        }

        return errorPositions.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    /**
     * 本地分析错误标签
     */
    private String analyzeErrorTagsLocal(String userText, String audioText) {
        List<String> errorTags = new ArrayList<>();

        // 检测平翘舌错误
        if (containsAny(userText, "zh,ch,sh") && containsAny(audioText, "z,c,s") ||
            containsAny(audioText, "zh,ch,sh") && containsAny(userText, "z,c,s")) {
            errorTags.add("平翘舌");
        }

        // 检测前后鼻音错误
        if (containsAny(userText, "ing,eng") && containsAny(audioText, "in,en") ||
            containsAny(audioText, "ing,eng") && containsAny(userText, "in,en")) {
            errorTags.add("前后鼻音");
        }

        // 检测n/l混淆
        if (containsAny(userText, "n") && containsAny(audioText, "l") ||
            containsAny(audioText, "n") && containsAny(userText, "l")) {
            errorTags.add("nl混淆");
        }

        // 检测h/f混淆
        if (containsAny(userText, "h") && containsAny(audioText, "f") ||
            containsAny(audioText, "h") && containsAny(userText, "f")) {
            errorTags.add("hf混淆");
        }

        // 检测送气音和不送气音混淆
        if (containsAny(userText, "b,d,g") && containsAny(audioText, "p,t,k") ||
            containsAny(audioText, "b,d,g") && containsAny(userText, "p,t,k")) {
            errorTags.add("送气音");
        }

        if (errorTags.isEmpty()) {
            errorTags.add("发音错误");
        }

        return String.join(",", errorTags);
    }

    /**
     * 分析错误并返回错误位置和错误标签
     *
     * @param userContent   用户语音识别结果
     * @param audioContent  标准音频文本
     * @return 包含错误位置和错误标签的数组 [errorPositions, errorTags]
     */
    private String[] analyzeErrors(String userContent, String audioContent) {
        String[] result = new String[]{"", ""};

        if (userContent == null || audioContent == null || userContent.isEmpty() || audioContent.isEmpty()) {
            return result;
        }

        // 去除非中文字符
        String userText = userContent.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");
        String audioText = audioContent.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");

        // 找出错误位置
        List<Integer> errorPositions = new ArrayList<>();
        StringBuilder errorPositionsBuilder = new StringBuilder();

        int minLen = Math.min(userText.length(), audioText.length());
        for (int i = 0; i < minLen; i++) {
            if (userText.charAt(i) != audioText.charAt(i)) {
                errorPositions.add(i + 1); // 位置从1开始
            }
        }

        // 如果长度不同，剩余的也算错误
        if (userText.length() > audioText.length()) {
            for (int i = audioText.length(); i < userText.length(); i++) {
                errorPositions.add(i + 1);
            }
        } else if (audioText.length() > userText.length()) {
            for (int i = userText.length(); i < audioText.length(); i++) {
                errorPositions.add(i + 1);
            }
        }

        if (!errorPositions.isEmpty()) {
            errorPositionsBuilder.append(String.join(",", errorPositions.stream().map(String::valueOf).collect(Collectors.toList())));
        }

        // 分析错误标签（基于简单的声母韵母错误模式）
        List<String> errorTags = new ArrayList<>();

        // 检测平翘舌错误 (zh,ch,sh vs z,c,s)
        if (containsAny(userText, "zh,ch,sh") && containsAny(audioText, "z,c,s") ||
            containsAny(audioText, "zh,ch,sh") && containsAny(userText, "z,c,s")) {
            errorTags.add("平翘舌");
        }

        // 检测前后鼻音错误 (ing,eng vs in,en)
        if (containsAny(userText, "ing,eng") && containsAny(audioText, "in,en") ||
            containsAny(audioText, "ing,eng") && containsAny(userText, "in,en")) {
            errorTags.add("前后鼻音");
        }

        // 检测n/l混淆
        if (containsAny(userText, "n") && containsAny(audioText, "l") ||
            containsAny(audioText, "n") && containsAny(userText, "l")) {
            errorTags.add("nl混淆");
        }

        // 检测h/f混淆
        if (containsAny(userText, "h") && containsAny(audioText, "f") ||
            containsAny(audioText, "h") && containsAny(userText, "f")) {
            errorTags.add("hf混淆");
        }

        // 检测送气音和不送气音混淆 (b,d,g vs p,t,k)
        if (containsAny(userText, "b,d,g") && containsAny(audioText, "p,t,k") ||
            containsAny(audioText, "b,d,g") && containsAny(userText, "p,t,k")) {
            errorTags.add("送气音");
        }

        // 如果有错误但没识别出具体标签，标记为一般错误
        if (!errorPositions.isEmpty() && errorTags.isEmpty()) {
            errorTags.add("发音错误");
        }

        String errorTagsStr = String.join(",", errorTags);

        result[0] = errorPositionsBuilder.toString();
        result[1] = errorTagsStr;

        return result;
    }

    /**
     * 检查字符串是否包含指定的字符序列
     */
    private boolean containsAny(String text, String chars) {
        String[] charArray = chars.split(",");
        for (String c : charArray) {
            if (text.contains(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成结果分析
     *
     * @param score 得分
     * @param errorTags 错误标签
     * @param itemCount 总题数
     * @return 结果分析文本
     */
    private String generateResultAnalysis(float score, String errorTags, int itemCount) {
        StringBuilder analysis = new StringBuilder();

        // 总体评价
        if (score >= 90) {
            analysis.append("您的发音非常标准，继续保持！");
        } else if (score >= 80) {
            analysis.append("您的发音整体良好，个别细节需要注意。");
        } else if (score >= 70) {
            analysis.append("您的发音基本准确，但存在一些常见问题需要改进。");
        } else if (score >= 60) {
            analysis.append("您的发音存在较多问题，建议加强练习。");
        } else {
            analysis.append("您的发音需要重点练习，建议从基础开始。");
        }

        // 添加错误标签分析
        if (errorTags != null && !errorTags.isEmpty()) {
            analysis.append("主要问题：");
            String[] tags = errorTags.split(",");
            for (int i = 0; i < tags.length; i++) {
                analysis.append(tags[i]);
                if (i < tags.length - 1) {
                    analysis.append("、");
                }
            }
            analysis.append("。");
        }

        return analysis.toString();
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

        // 构造FFmpeg命令，修改采样率为16kHz，16位，同时获取时长
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
