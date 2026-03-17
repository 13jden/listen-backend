package com.example.wx.elasticsearch.service;

import com.example.wx.elasticsearch.entity.*;
import com.example.wx.elasticsearch.repository.*;
import com.example.wx.mapper.AudioMapper;
import com.example.wx.mapper.TestdetailMapper;
import com.example.wx.mapper.UsertestMapper;
import com.example.wx.mapper.UserMapper;
import com.example.wx.pojo.Audio;
import com.example.wx.pojo.Testdetail;
import com.example.wx.pojo.Usertest;
import com.example.wx.pojo.User;
import com.example.common.dto.TestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ES数据同步服务
 * 负责将MySQL数据同步到ES
 * 核心功能：
 * 1. 单次测试完成后实时同步到ES
 * 2. 定时增量同步（按日期）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchSyncService {

    private final UserMapper userMapper;
    private final UsertestMapper usertestMapper;
    private final TestdetailMapper testdetailMapper;
    private final AudioMapper audioMapper;

    private final TestEsRepository testEsRepository;
    private final TestItemEsRepository testItemEsRepository;

    /**
     * 每天凌晨2点执行增量同步（按日期）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledSyncByDate() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String dateStr = yesterday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        log.info("开始定时同步，日期: {}", dateStr);
        syncByDate(dateStr);
    }

    /**
     * 按指定日期同步数据
     */
    @Transactional(readOnly = true)
    public void syncByDate(String dateStr) {
        log.info("开始同步日期 {} 的数据", dateStr);

        try {
            Date startDate = java.sql.Date.valueOf(LocalDate.parse(dateStr));
            Date endDate = java.sql.Date.valueOf(LocalDate.parse(dateStr).plusDays(1));

            // 同步该日期的测试数据（包含汇总和详情）
            List<Usertest> tests = usertestMapper.selectListByDate(startDate, endDate);
            for (Usertest usertest : tests) {
                syncTestToEs(usertest.getId());
            }
            log.info("日期 {} 测试数据同步完成，共同步 {} 条", dateStr, tests.size());

        } catch (Exception e) {
            log.error("日期 {} 数据同步失败: {}", dateStr, e.getMessage(), e);
        }
    }

    /**
     * 单次测试完成后同步到ES
     * 同步测试汇总数据和所有测试详情
     *
     * @param testId 测试ID
     */
    public void syncTestToEs(String testId) {
        log.info("开始同步测试 {} 到ES", testId);
        try {
            // 查询测试数据
            Usertest usertest = usertestMapper.selectById(testId);
            if (usertest == null) {
                log.warn("测试 {} 不存在，跳过同步", testId);
                return;
            }

            // 查询关联的用户信息
            User user = userMapper.selectById(usertest.getUserId());

            // 转换并保存测试汇总数据
            TestEs testEs = convertToTestEs(usertest, user);
            testEsRepository.save(testEs);

            // 查询测试详情并同步
            List<Testdetail> details = testdetailMapper.selectListByTestId(testId);
            for (Testdetail detail : details) {
                TestItemEs itemEs = convertToTestItemEs(detail);
                testItemEsRepository.save(itemEs);
            }

            log.info("测试 {} 同步完成，包含 {} 条详情", testId, details.size());
        } catch (Exception e) {
            log.error("测试 {} 同步失败: {}", testId, e.getMessage(), e);
        }
    }

    /**
     * 单条语音上传后同步到ES
     *
     * @param testDto 测试DTO
     */
    public void syncTestItemToEs(TestDto testDto) {
        log.info("同步单条测试详情到ES: {}", testDto.getId());
        try {
            TestItemEs itemEs = convertDtoToTestItemEs(testDto);
            testItemEsRepository.save(itemEs);
            log.info("测试详情 {} 同步成功", testDto.getId());
        } catch (Exception e) {
            log.error("测试详情 {} 同步失败: {}", testDto.getId(), e.getMessage(), e);
        }
    }

    /**
     * Usertest + User -> TestEs 转换
     */
    private TestEs convertToTestEs(Usertest usertest, User user) {
        TestEs testEs = new TestEs();
        testEs.setTestId(usertest.getId());
        testEs.setUserId(usertest.getUserId());
        testEs.setTotalScore(usertest.getAvgScore());
        testEs.setResultAnalysis(usertest.getResultAnalysis());
        testEs.setPassFlag(usertest.getAvgScore() >= 60);

        // 设置用户基础信息
        if (user != null) {
            testEs.setMedicalId(user.getMedicalId());
            // User表暂无age和sex字段，如有需要可后续扩展
            // testEs.setAge(user.getAge());
            // testEs.setSex(user.getSex());
            if (user.getHospitalId() != null) {
                testEs.setHospitalId(String.valueOf(user.getHospitalId()));
            }
            // User表暂无hospitalName字段，如有需要可后续扩展
        }

        // 获取该测试的所有详情，计算总题目数量
        List<Testdetail> details = testdetailMapper.selectListByTestId(usertest.getId());
        if (details != null && !details.isEmpty()) {
            testEs.setTotalItems(details.size());
        }

        if (usertest.getTestTime() != null) {
            LocalDateTime testDateTime = usertest.getTestTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            testEs.setStartTime(testDateTime);
            testEs.setTestDate(testDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            testEs.setTestMonth(testDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM")));
        }

        if (usertest.getEndTime() != null) {
            LocalDateTime endDateTime = usertest.getEndTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            testEs.setEndTime(endDateTime);
            if (testEs.getStartTime() != null) {
                long duration = java.time.Duration.between(testEs.getStartTime(), endDateTime).getSeconds();
                testEs.setDurationSec((int) duration);
            }
        }

        testEs.setCompletionStatus(usertest.getNum() != null && usertest.getNum() > 0 ? "completed" : "in_progress");
        testEs.setItemCount(usertest.getNum());

        return testEs;
    }

    /**
     * Testdetail -> TestItemEs 转换
     */
    private TestItemEs convertToTestItemEs(Testdetail detail) {
        TestItemEs itemEs = new TestItemEs();
        itemEs.setId(detail.getId());
        itemEs.setTestId(detail.getTestId());
        itemEs.setItemIndex(detail.getIndex());
        itemEs.setAudioId(detail.getAudioId());
        itemEs.setUserAsrText(detail.getUserContent());
        itemEs.setFinalScore(detail.getScore() != null ? detail.getScore().floatValue() : null);
        itemEs.setIsCorrect(detail.getScore() != null && detail.getScore() >= 60);

        // 设置错误信息
        if (detail.getErrorTags() != null && !detail.getErrorTags().isEmpty()) {
            itemEs.setErrorTags(detail.getErrorTags().split(","));
        }
        itemEs.setErrorDetail(detail.getErrorPositions());
        itemEs.setResultAnalysis(detail.getResultAnalysis());

        // 设置时长信息
        itemEs.setSpeechDurationSec(detail.getSpeechDurationSec());
        itemEs.setStandardDurationSec(detail.getStandardDurationSec());
        itemEs.setDurationScore(detail.getDurationScore());

        if (detail.getTestTime() != null) {
            itemEs.setCreatedAt(detail.getTestTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }

        // 查询标准音频文本
        Audio audio = audioMapper.selectById(detail.getAudioId());
        if (audio != null) {
            itemEs.setStandardText(audio.getContent());
        }

        return itemEs;
    }

    /**
     * TestDto -> TestItemEs 转换
     */
    private TestItemEs convertDtoToTestItemEs(TestDto dto) {
        TestItemEs itemEs = new TestItemEs();
        itemEs.setId(dto.getId());
        itemEs.setTestId(dto.getTestId());
        itemEs.setItemIndex(dto.getIndex());
        itemEs.setUserAsrText(dto.getUserText());
        itemEs.setStandardText(dto.getTestText());
        itemEs.setFinalScore(dto.getScore());
        itemEs.setIsCorrect(dto.getScore() >= 60);

        // 设置错误信息
        if (dto.getErrorTags() != null && !dto.getErrorTags().isEmpty()) {
            itemEs.setErrorTags(dto.getErrorTags().split(","));
        }
        itemEs.setErrorDetail(dto.getErrorPositions());
        itemEs.setResultAnalysis(dto.getResultAnalysis());

        // 设置时长信息
        itemEs.setSpeechDurationSec(dto.getSpeechDurationSec());
        itemEs.setStandardDurationSec(dto.getStandardDurationSec());
        itemEs.setDurationScore(dto.getDurationScore());

        // 设置评分相关信息
        // editDistanceScore 和 textSimilarity 可以通过计算得出，这里暂未存储
        // llmScore 对应 aiScore
        itemEs.setLlmScore(dto.getAiScore());

        if (dto.getTestTime() != null) {
            itemEs.setCreatedAt(dto.getTestTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }

        return itemEs;
    }

    /**
     * 根据测试ID查询ES中的测试
     */
    public TestEs findTestById(String testId) {
        return testEsRepository.findById(testId).orElse(null);
    }
}
