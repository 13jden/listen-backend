package com.example.wx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.common.getHttpAudio;
import com.example.common.constants.Constants;
import com.example.common.dto.AudioDto;
import com.example.common.dto.AudioUploadResponse;
import com.example.common.redis.RedisComponent;
import com.example.common.utils.CopyTools;
import com.example.common.utils.StringTools;
import com.example.wx.mapper.AdminMapper;
import com.example.wx.mapper.AudioMapper;
import com.example.wx.pojo.Audio;
import com.example.wx.service.AudioService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AudioServiceImpl extends ServiceImpl<AudioMapper, Audio> implements AudioService {

    private final AudioMapper audioMapper;
    private final RedisComponent redisComponent;
    private final AdminMapper adminMapper;

    @Value("${upload.path}")
    private String audioPath;

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Override
    public List<Audio> getAudio(int num) {
        return audioMapper.getRandomAudio(num);
    }

    @Override
    public AudioUploadResponse uploadAudio(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = StringTools.getRandomBumber(10) + extension;
        String fullPath = audioPath + "/" + fileName;

        File targetFile = new File(fullPath);
        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }
        try {
            file.transferTo(targetFile);
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败", e);
        }

        Float durationSec = getAudioDuration(fullPath);

        return AudioUploadResponse.builder()
                .fileName(fileName)
                .durationSec(durationSec)
                .build();
    }

    @Override
    public AudioDto save(String content, String adminId, String fileName, Float durationSec) {
        String newFilePath = audioPath + "/" + fileName;
        Audio audio = new Audio();
        audio.setContent(content);
        audio.setPath(newFilePath);
        audio.setAdminId(adminId);
        audio.setUploadTime(new Date());
        audio.setId(StringTools.getRandomBumber(Constants.LENGTH_10));
        audio.setDurationSec(durationSec);

        if (audioMapper.insert(audio) > 0) {
            return toAudioDto(audio);
        }
        return null;
    }

    @Override
    public AudioDto updateAudio(String id, String content, String path) {
        Audio existingAudio = audioMapper.selectById(id);
        if (existingAudio == null) {
            return null;
        }

        if (content != null) {
            existingAudio.setContent(content);
        }

        if (path != null && !path.isEmpty()) {
            if (existingAudio.getPath() != null) {
                deleteFile(existingAudio.getPath());
            }
            existingAudio.setPath(path);
        }

        audioMapper.updateById(existingAudio);
        return toAudioDto(existingAudio);
    }

    @Override
    public boolean deleteAudio(String id) {
        Audio audio = audioMapper.selectById(id);
        if (audio != null) {
            if (audio.getPath() != null) {
                deleteFile(audio.getPath());
            }
            audioMapper.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteTempFile(String fileName) {
        String filePath = audioPath + "/" + fileName;
        File file = new File(filePath);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    private Float getAudioDuration(String filePath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(ffmpegPath, "-i", filePath);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            process.waitFor();

            Pattern pattern = Pattern.compile("Duration: (\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{2})");
            Matcher matcher = pattern.matcher(output.toString());
            if (matcher.find()) {
                int hours = Integer.parseInt(matcher.group(1));
                int minutes = Integer.parseInt(matcher.group(2));
                int seconds = Integer.parseInt(matcher.group(3));
                int centiseconds = Integer.parseInt(matcher.group(4));
                return (float) (hours * 3600 + minutes * 60 + seconds + centiseconds / 100.0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
    }

    private AudioDto toAudioDto(Audio audio) {
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
    }

    @Override
    public IPage<AudioDto> getListAudio(int pageNum, int pageSize) {
        Page<Audio> audioPage = new Page<>(pageNum, pageSize);
        IPage<Audio> audioIPage = audioMapper.selectPage(audioPage, new QueryWrapper<Audio>().orderByDesc("upload_time"));
        return audioIPage.convert(this::toAudioDto);
    }

    @Override
    public IPage<AudioDto> searchAudio(String keyWord, int pageNum, int pageSize) {
        Page<Audio> audioPage = new Page<>(pageNum, pageSize);
        QueryWrapper<Audio> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("content", keyWord).orderByDesc("upload_time");

        IPage<Audio> audioIPage = audioMapper.selectPage(audioPage, queryWrapper);
        return audioIPage.convert(this::toAudioDto);
    }
}
