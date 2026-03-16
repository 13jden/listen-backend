package com.example.common.elasticsearch.service;

import com.example.common.elasticsearch.dto.SyncDTO;
import com.example.common.elasticsearch.entity.*;
import com.example.common.elasticsearch.repository.*;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ES数据同步服务
 * 负责将MySQL数据同步到ES
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchSyncService {

    private final UserMapper userMapper;
    private final UsertestMapper usertestMapper;
    private final TestdetailMapper testdetailMapper;
    private final AudioMapper audioMapper;

    private final UserEsRepository userEsRepository;
    private final TestEsRepository testEsRepository;
    private final TestItemEsRepository testItemEsRepository;
    private final AudioEsRepository audioEsRepository;

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
            
            // 同步该日期的测试数据
            List<Usertest> tests = usertestMapper.selectListByDate(startDate, endDate);
            List<TestEs> testEsList = tests.stream().map(this::convertToTestEs).collect(Collectors.toList());
            testEsRepository.saveAll(testEsList);
            log.info("日期 {} 测试数据同步完成，共同步 {} 条", dateStr, testEsList.size());
            
            // 同步该日期的测试详情
            List<Testdetail> details = testdetailMapper.selectListByDate(startDate, endDate);
            List<TestItemEs> testItemEsList = details.stream().map(this::convertToTestItemEs).collect(Collectors.toList());
            testItemEsRepository.saveAll(testItemEsList);
            log.info("日期 {} 测试详情同步完成，共同步 {} 条", dateStr, testItemEsList.size());
            
        } catch (Exception e) {
            log.error("日期 {} 数据同步失败: {}", dateStr, e.getMessage(), e);
        }
    }

    /**
     * 根据TestDto同步单条测试详情到ES
     * 用于测试完成后实时同步
     */
    public void syncTestItem(TestDto testDto) {
        log.info("根据DTO同步测试详情: {}", testDto.getId());
        try {
            TestItemEs testItemEs = convertDtoToTestItemEs(testDto);
            testItemEsRepository.save(testItemEs);
            log.info("测试详情 {} 同步成功", testItemEs.getId());
        } catch (Exception e) {
            log.error("测试详情 {} 同步失败: {}", testDto.getId(), e.getMessage(), e);
        }
    }

    /**
     * 全量同步所有数据
     */
    @Transactional(readOnly = true)
    public void syncAll() {
        log.info("开始全量同步");
        
        syncUsers();
        syncTests();
        syncTestItems();
        syncAudios();
        
        log.info("全量数据同步完成");
    }

    /**
     * 根据DTO同步指定数据
     */
    @Transactional(readOnly = true)
    public void syncByDTO(SyncDTO dto) {
        log.info("开始根据DTO同步: {}", dto);
        
        if (dto.getSyncUsers() != null && dto.getSyncUsers()) {
            syncUsers();
        }
        if (dto.getSyncTests() != null && dto.getSyncTests()) {
            if (dto.getStartDate() != null && dto.getEndDate() != null) {
                syncTestsByDateRange(dto.getStartDate(), dto.getEndDate());
            } else {
                syncTests();
            }
        }
        if (dto.getSyncTestItems() != null && dto.getSyncTestItems()) {
            if (dto.getStartDate() != null && dto.getEndDate() != null) {
                syncTestItemsByDateRange(dto.getStartDate(), dto.getEndDate());
            } else {
                syncTestItems();
            }
        }
        if (dto.getSyncAudios() != null && dto.getSyncAudios()) {
            syncAudios();
        }
        
        log.info("DTO同步完成");
    }

    /**
     * 同步用户数据到ES
     */
    @Transactional(readOnly = true)
    public void syncUsers() {
        log.info("开始同步用户数据到ES");
        List<User> users = userMapper.selectList(null);
        
        List<UserEs> userEsList = users.stream().map(this::convertToUserEs).collect(Collectors.toList());
        userEsRepository.saveAll(userEsList);
        
        log.info("用户数据同步完成，共同步 {} 条", userEsList.size());
    }

    /**
     * 同步测试数据到ES
     */
    @Transactional(readOnly = true)
    public void syncTests() {
        log.info("开始同步测试数据到ES");
        List<Usertest> tests = usertestMapper.selectList(null);
        
        List<TestEs> testEsList = tests.stream().map(this::convertToTestEs).collect(Collectors.toList());
        testEsRepository.saveAll(testEsList);
        
        log.info("测试数据同步完成，共同步 {} 条", testEsList.size());
    }

    /**
     * 按日期范围同步测试数据
     */
    @Transactional(readOnly = true)
    public void syncTestsByDateRange(Date startDate, Date endDate) {
        log.info("开始同步日期范围的测试数据: {} - {}", startDate, endDate);
        List<Usertest> tests = usertestMapper.selectListByDate(startDate, endDate);
        
        List<TestEs> testEsList = tests.stream().map(this::convertToTestEs).collect(Collectors.toList());
        testEsRepository.saveAll(testEsList);
        
        log.info("测试数据同步完成，共同步 {} 条", testEsList.size());
    }

    /**
     * 同步测试详情数据到ES
     */
    @Transactional(readOnly = true)
    public void syncTestItems() {
        log.info("开始同步测试详情数据到ES");
        List<Testdetail> details = testdetailMapper.selectList(null);
        
        List<TestItemEs> testItemEsList = details.stream().map(this::convertToTestItemEs).collect(Collectors.toList());
        testItemEsRepository.saveAll(testItemEsList);
        
        log.info("测试详情数据同步完成，共同步 {} 条", testItemEsList.size());
    }

    /**
     * 按日期范围同步测试详情数据
     */
    @Transactional(readOnly = true)
    public void syncTestItemsByDateRange(Date startDate, Date endDate) {
        log.info("开始同步日期范围的测试详情数据: {} - {}", startDate, endDate);
        List<Testdetail> details = testdetailMapper.selectListByDate(startDate, endDate);
        
        List<TestItemEs> testItemEsList = details.stream().map(this::convertToTestItemEs).collect(Collectors.toList());
        testItemEsRepository.saveAll(testItemEsList);
        
        log.info("测试详情数据同步完成，共同步 {} 条", testItemEsList.size());
    }

    /**
     * 同步音频数据到ES
     */
    @Transactional(readOnly = true)
    public void syncAudios() {
        log.info("开始同步音频数据到ES");
        List<Audio> audios = audioMapper.selectList(null);
        
        List<AudioEs> audioEsList = audios.stream().map(this::convertToAudioEs).collect(Collectors.toList());
        audioEsRepository.saveAll(audioEsList);
        
        log.info("音频数据同步完成，共同步 {} 条", audioEsList.size());
    }

    /**
     * User -> UserEs 转换
     */
    private UserEs convertToUserEs(User user) {
        UserEs userEs = new UserEs();
        userEs.setUserId(user.getUserId());
        userEs.setOpenid(user.getOpenId());
        userEs.setMedicalId(user.getMedicalId());
        userEs.setPhone(user.getNumber());

        if (user.getHospitalId() != null) {
            userEs.setHospitalId(String.valueOf(user.getHospitalId()));
        }

        if (user.getRegisterTime() != null) {
            userEs.setCreatedAt(user.getRegisterTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }

        userEs.setStatus("active");
        return userEs;
    }

    /**
     * Usertest -> TestEs 转换
     */
    private TestEs convertToTestEs(Usertest usertest) {
        TestEs testEs = new TestEs();
        testEs.setTestId(usertest.getId());
        testEs.setUserId(usertest.getUserId());
        testEs.setTotalScore(usertest.getAvgScore());

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

        if (detail.getTestTime() != null) {
            itemEs.setCreatedAt(detail.getTestTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }

        return itemEs;
    }

    /**
     * TestDto -> TestItemEs 转换
     * 用于业务层测试完成后实时同步
     */
    private TestItemEs convertDtoToTestItemEs(TestDto dto) {
        TestItemEs itemEs = new TestItemEs();
        itemEs.setId(dto.getId());
        itemEs.setTestId(dto.getTestId());
        itemEs.setItemIndex(dto.getIndex());
        itemEs.setUserAsrText(dto.getUserText());
        itemEs.setFinalScore((float) dto.getScore());
        itemEs.setIsCorrect(dto.getScore() >= 60);

        if (dto.getTestTime() != null) {
            itemEs.setCreatedAt(dto.getTestTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }

        return itemEs;
    }

    /**
     * Audio -> AudioEs 转换
     */
    private AudioEs convertToAudioEs(Audio audio) {
        AudioEs audioEs = new AudioEs();
        audioEs.setAudioId(audio.getId());
        audioEs.setContent(audio.getContent());
        audioEs.setPath(audio.getPath());
        audioEs.setUploadAdmin(audio.getAdminId());

        if (audio.getUploadTime() != null) {
            audioEs.setUploadTime(audio.getUploadTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }

        audioEs.setStatus("active");
        return audioEs;
    }

    /**
     * 根据用户ID查询ES中的用户
     */
    public UserEs findUserById(String userId) {
        return userEsRepository.findById(userId).orElse(null);
    }

    /**
     * 根据测试ID查询ES中的测试
     */
    public TestEs findTestById(String testId) {
        return testEsRepository.findById(testId).orElse(null);
    }

    /**
     * 删除ES中的用户
     */
    public void deleteUser(String userId) {
        userEsRepository.deleteById(userId);
    }

    /**
     * 删除ES中的测试
     */
    public void deleteTest(String testId) {
        testEsRepository.deleteById(testId);
    }
}
