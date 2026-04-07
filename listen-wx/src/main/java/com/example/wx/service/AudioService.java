package com.example.wx.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.common.dto.AudioDto;
import com.example.common.dto.AudioUploadResponse;
import com.example.wx.pojo.Audio;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author dzk
 * @since 2025-02-04
 */
public interface AudioService extends IService<Audio> {

    List<Audio> getAudio(int num);

    AudioUploadResponse uploadAudio(MultipartFile file);

    AudioDto save(String content, String adminId, String fileName, Float durationSec);

    AudioDto updateAudio(String id, String content, String path);

    boolean deleteAudio(String id);

    boolean deleteTempFile(String fileName);

    IPage<AudioDto> getListAudio(int pageNum, int pageSize);

    IPage<AudioDto> searchAudio(String keyWord, int pageNum, int pageSize);
}
