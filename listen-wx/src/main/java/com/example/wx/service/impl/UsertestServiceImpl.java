package com.example.wx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.common.Result;
import com.example.common.common.getHttpAudio;
import com.example.common.constants.Constants;
import com.example.common.dto.TestDto;
import com.example.common.dto.UserTestDto;
import com.example.common.utils.StringTools;
import com.example.wx.mapper.AudioMapper;
import com.example.wx.mapper.TestdetailMapper;
import com.example.wx.mapper.UserMapper;
import com.example.wx.pojo.Audio;
import com.example.wx.pojo.Testdetail;
import com.example.wx.pojo.User;
import com.example.wx.pojo.Usertest;
import com.example.wx.mapper.UsertestMapper;
import com.example.wx.service.UsertestService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
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
public class UsertestServiceImpl extends ServiceImpl<UsertestMapper, Usertest> implements UsertestService {

    @Autowired
    private UsertestMapper usertestMapper;

    @Autowired
    private AudioMapper audioMapper;

    @Autowired
    private TestdetailMapper testdetailMapper;

    @Autowired
    private UserMapper userMapper;

    @Value("${userFile.path}")
    private String userPath;

    @Override
    public List<TestDto> getTest(String userId , int num) {
        //新建用户测试
        Usertest usertest = new Usertest();
        String testId = StringTools.getRandomBumber(Constants.LENGTH_10);
        usertest.setUserId(userId);
        usertest.setAvgScore(0);
        usertest.setId(testId);
        usertest.setTestTime(new Date());
        // 生成目录名
        User user = userMapper.selectById(usertest.getUserId());
        String directoryName = userPath + "/" + user.getMedicalId() + new Date().getTime();
        // 创建File对象
        File directory = new File(directoryName);
        boolean isCreated = directory.mkdirs();
        if(!isCreated){
            return null;
        }
        usertest.setTestFilePath(directoryName);
        System.out.println(usertest.getTestFilePath());
        usertest.setNum(0);
        usertestMapper.insert(usertest);

        //创建返回dto
        List<TestDto> testDtoList = new ArrayList<>();
        //获取测试音频
        List<Audio> audioList= audioMapper.getRandomAudio(num);
        int i = 0;
        for(Audio audio : audioList){
            i++;
            //新建测试详细
            TestDto testDto =new TestDto();
            Testdetail testdetail = new Testdetail();
            testdetail.setId(StringTools.getRandomBumber(Constants.LENGTH_15));
            testdetail.setTestId(testId);
            testdetail.setAudioId(audio.getId());
            testdetail.setIndex(i);
            testdetail.setScore(0);
            testdetailMapper.insertTestDetail(testdetail);
            //添加返回数据
            testDto.setId(testdetail.getId());
            testDto.setTestId(testId);
            testDto.setAudioPath(getHttpAudio.getAudioUrl(audio.getPath()));
            testDto.setTestAudioPath(getHttpAudio.getAudioUrl(testdetail.getUserAudioPath()));
            testDtoList.add(testDto);
        }
        return testDtoList;
    }

    public Result isTestContinue(String userId,int num,boolean isContinue,int time) {
        Usertest usertest = usertestMapper.selectByUserId(userId,num);
        //如果没有查到或者不再继续,直接新的测试
        if(usertest==null||!isContinue){
            if(usertest!=null){
                //删除旧的文件夹和数据库内容；
                String path=usertest.getTestFilePath();
                System.out.println(path);
                Path directoryPath = Paths.get(path);
                try {
                    Files.walkFileTree(directoryPath, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file); // 删除文件
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir); // 删除目录
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String testId = usertest.getId();
                List<Testdetail> testdetailList = testdetailMapper.selectList(testId);
                for(Testdetail t : testdetailList){
                    testdetailMapper.deleteById(t);
                }
                usertestMapper.deleteById(usertest);
            }


            return Result.success(getTest(userId,num));
        }
        //如果查到了选择继续（第一次需要确认是否继续）
        if(time==1){
            return Result.success(usertest);
        }
        List<Testdetail> testdetailList = testdetailMapper.selectList(usertest.getId());
        List<TestDto> testDtoList = new ArrayList<>();
        for(Testdetail testdetail:testdetailList){
            TestDto testDto =new TestDto();
            testDto.setTestId(testdetail.getTestId());
            Audio audio = audioMapper.selectById(testdetail.getAudioId());
            testDto.setAudioPath(getHttpAudio.getAudioUrl(audio.getPath()));
            testDto.setTestAudioPath(getHttpAudio.getAudioUrl(testdetail.getUserAudioPath()));
            testDto.setScore(testdetail.getScore());
            testDto.setTestTime(testdetail.getTestTime());
            testDto.setId(testdetail.getId());
            testDto.setIndex(testdetail.getIndex());
            testDtoList.add(testDto);
        }
        return Result.success(testDtoList);
    }

    @Override
    public Usertest uploadAll(String testId) {
        Usertest usertest = usertestMapper.selectById(testId);
        if(usertest==null||usertest.getAvgScore()!=0)
            throw new RuntimeException("提交错误");
        List<Testdetail> testdetailList = new ArrayList<>();
        testdetailList = testdetailMapper.selectList(testId);
        int sum = 0;
        double avg = 0;
        int n=0;
        for(Testdetail testdetail : testdetailList){
            sum+=testdetail.getScore();
            n++;
        }
        avg =  sum/n;
        usertest.setEndTime(new Date());
        BigDecimal avgBigDecimal = new BigDecimal(avg).setScale(2, RoundingMode.HALF_UP);
        usertest.setAvgScore(avgBigDecimal.doubleValue());
        usertest.setEndTime(new Date());
        usertest.setNum(n);
        usertestMapper.updateById(usertest);
        return usertest;
    }

    @Override
    public List<Usertest> getMyTest(String userId) {
        return usertestMapper.getTestByUserId(userId);
    }


    @Override
    public IPage<UserTestDto> getUserTest(int pageNum, int pageSize) {
        Page<Usertest> usertestPage = new Page<>(pageNum,pageSize);
        IPage<Usertest> usertestIPage = usertestMapper.selectPage(usertestPage,null);
        return usertestIPage.convert(usertest -> {
            UserTestDto userTestDto = new UserTestDto();
            User user = userMapper.selectById(usertest.getUserId());
            userTestDto.setMedicalId(user.getMedicalId());
            userTestDto.setName(user.getName());
            userTestDto.setStarTime(usertest.getTestTime());
            userTestDto.setEndTime(usertest.getEndTime());
            userTestDto.setScore(usertest.getAvgScore());
            userTestDto.setTestId(usertest.getId());
            return userTestDto;
        });
    }

    @Override
    public IPage<UserTestDto> searchTest(String keyWord, int pageNum, int pageSize) {
        Page<UserTestDto> resultPage = new Page<>(pageNum, pageSize);
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.like("name", keyWord).or().like("medical_id", keyWord);
        List<User> userList = userMapper.selectList(userQueryWrapper);
        if (userList.isEmpty()) {
            return resultPage;
        }
        List<String> userIds = userList.stream().map(User::getUserId).collect(Collectors.toList());
        QueryWrapper<Usertest> usertestQueryWrapper = new QueryWrapper<>();
        usertestQueryWrapper.in("user_id", userIds);
        Page<Usertest> usertestPage = new Page<>(pageNum, pageSize);
        IPage<Usertest> usertestIPage = usertestMapper.selectPage(usertestPage, usertestQueryWrapper);
        return usertestIPage.convert(usertest -> {
            UserTestDto userTestDto = new UserTestDto();
            User user = userMapper.selectById(usertest.getUserId());
            userTestDto.setMedicalId(user.getMedicalId());
            userTestDto.setName(user.getName());
            userTestDto.setStarTime(usertest.getTestTime());
            userTestDto.setEndTime(usertest.getEndTime());
            userTestDto.setScore(usertest.getAvgScore());
            userTestDto.setTestId(usertest.getId());
            return userTestDto;
        });
    }
}
