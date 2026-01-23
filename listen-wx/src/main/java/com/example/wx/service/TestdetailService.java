package com.example.wx.service;

import com.example.common.common.Result;
import com.example.common.dto.UserDetailInfo;
import com.example.wx.pojo.Testdetail;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author dzk
 * @since 2025-02-06
 */
public interface TestdetailService extends IService<Testdetail> {

    Result OneUserAudioUpload(MultipartFile testAudio, String testDetailId) throws Exception;

    Result getTestDetail(String testId);

    Result getPreScore(MultipartFile testAudio);

    List<UserDetailInfo> getUserDetail(String testId);

}
