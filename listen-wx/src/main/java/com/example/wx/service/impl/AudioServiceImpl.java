package com.example.wx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.common.getHttpAudio;
import com.example.common.constants.Constants;
import com.example.common.dto.AudioDto;
import com.example.common.redis.RedisComponent;
import com.example.common.utils.CopyTools;
import com.example.common.utils.StringTools;
import com.example.wx.mapper.AdminMapper;
import com.example.wx.pojo.Audio;
import com.example.wx.mapper.AudioMapper;
import com.example.wx.service.AudioService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author dzk
 * @since 2025-02-04
 */
@Service
public class AudioServiceImpl extends ServiceImpl<AudioMapper, Audio> implements AudioService {

    @Autowired
    private AudioMapper audioMapper;

    @Autowired
    RedisComponent redisComponent;

    @Autowired
    private AdminMapper adminMapper;

    @Override
    public List<Audio> getAudio(int num) {
        return audioMapper.getRandomAudio(num);
    }

    @Override
    public Audio save(String content, String adminId, String newFilePath) {
        // 保存到数据库
        Audio audio = new Audio();
        audio.setContent(content);
        audio.setPath(newFilePath);
        audio.setAdminId(adminId);
        audio.setUploadTime(new Date());
        audio.setId(StringTools.getRandomBumber(Constants.LENGTH_10));
//        redisComponent.deleteAudio(audio.getId());
//        System.out.println("删除");
        if(audioMapper.insert(audio)>0)
            return audio;
        else
            return null;

    }

    @Override
    public IPage<AudioDto> getListAudio(int pageNum, int pageSize) {
        Page<Audio> audioPage = new Page<>(pageNum, pageSize);
        // 查询所有音频记录，并按上传时间降序排序
        IPage<Audio> audioIPage = audioMapper.selectPage(audioPage, new QueryWrapper<Audio>().orderByDesc("upload_time"));
        return audioIPage.convert(audio -> {
            AudioDto audioDto = redisComponent.getAudio(audio.getId());
            if(audioDto==null){
                audioDto = CopyTools.copy(audio,AudioDto.class);
                audioDto.setTestTimes(audioMapper.getTestTimes(audio.getId()));
                audioDto.setPath(getHttpAudio.getAudioUrl(audio.getPath()));
                System.out.println(audioDto.getPath());
                audioDto.setScore(audioMapper.getTestScore(audio.getId()));
                audioDto.setTime(audio.getUploadTime());
                audioDto.setAdminName(adminMapper.selectById(audio.getAdminId()).getName());
                redisComponent.setAudio(audio.getId(),audioDto);
            }
            return audioDto;
        });
    }

    @Override
    public IPage<AudioDto> searchAudio(String keyWord, int pageNum, int pageSize) {
        // 创建分页对象
        Page<Audio> audioPage = new Page<>(pageNum, pageSize);

        // 构建查询条件：对 content 字段进行模糊查询
        QueryWrapper<Audio> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("content", keyWord) // 模糊查询 content 字段
                .orderByDesc("upload_time"); // 按上传时间降序排序

        // 执行分页查询
        IPage<Audio> audioIPage = audioMapper.selectPage(audioPage, queryWrapper);

        return audioIPage.convert(audio -> {
            AudioDto audioDto = redisComponent.getAudio(audio.getId());
            if (audioDto == null) {
                audioDto = CopyTools.copy(audio, AudioDto.class);
                audioDto.setTestTimes(audioMapper.getTestTimes(audio.getId()));
                audioDto.setPath(getHttpAudio.getAudioUrl(audio.getPath()));
                audioDto.setScore(audioMapper.getTestScore(audio.getId()));
                audioDto.setTime(audio.getUploadTime());
                audioDto.setAdminName(adminMapper.selectById(audio.getAdminId()).getName());
                redisComponent.setAudio(audio.getId(), audioDto);
            }
            return audioDto;
        });
    }
}
