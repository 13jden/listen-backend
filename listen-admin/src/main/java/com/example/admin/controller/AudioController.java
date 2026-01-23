package com.example.admin.controller;


import com.example.common.common.Result;
import com.example.common.utils.StringTools;
import com.example.wx.pojo.Audio;
import com.example.wx.service.AudioService;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author dzk
 * @since 2025-02-04
 */
@RestController
@RequestMapping("/audio")
public class AudioController {

    @Autowired
    private AudioService audioService;

    @Value("${temp.path}")
    private String tempPath;

    /**
     * 获取音频列表（分页）
     */
    @RequestMapping("list")
    public Result getList(@RequestParam int pageNum,
                          @RequestParam int pageSize) {
        return Result.success(audioService.getListAudio(pageNum, pageSize));
    }

    /**
     * 搜索音频（模糊查询）
     */
    @RequestMapping("search")
    public Result searchAudio(@RequestParam String keyWord,
                              @RequestParam int pageNum,
                              @RequestParam int pageSize) {
        return Result.success(audioService.searchAudio(keyWord, pageNum, pageSize));
    }

    /**
     * 上传音频文件（临时文件）
     */
    @RequestMapping("uploadFile")
    public Result uploadAudio(@RequestParam("testAudio") MultipartFile testAudio) {
        if (testAudio.isEmpty()) {
            return Result.error("上传文件为空");
        }

        try {
            // 生成随机文件名
            String originalFilename = testAudio.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = StringTools.getRandomBumber(10) + extension;
            String fullPath = tempPath + "/" + fileName;

            // 保存文件
            File targetFile = new File(fullPath);
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }
            testAudio.transferTo(targetFile);

            return Result.success(fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败");
        }
    }

    /**
     * 添加音频
     */
    @RequestMapping("addFile")
    public Result addAudio(@RequestParam String content,
                           @RequestParam String adminId,
                           @RequestParam String fileName) {
        // 构建完整路径
        String newFilePath = tempPath + "/" + fileName;
        // 保存到数据库
        Audio audio = audioService.save(content, adminId, newFilePath);
        if (audio != null) {
            return Result.success(audio);
        } else {
            return Result.error("添加音频失败");
        }
    }

    /**
     * 删除临时文件
     */
    @RequestMapping("deleteTempFile")
    public Result deleteTempFile(@RequestParam String fileName) {
        String filePath = tempPath + "/" + fileName;
        File file = new File(filePath);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败");
            }
        }
        return Result.error("文件不存在");
    }

    /**
     * 删除音频
     */
    @RequestMapping("deleteFile")
    public Result deleteAudio(@RequestParam String id) {
        // 获取音频信息
        Audio audio = audioService.getById(id);
        if (audio != null) {
            // 删除服务器上的文件
            String filePath = audio.getPath();
            if (filePath != null) {
                File file = new File(filePath);
                if (file.exists()) {
                    file.delete();
                }
            }
            // 删除数据库记录
            audioService.removeById(id);
            return Result.success("删除成功");
        }
        return Result.error("音频不存在");
    }

    /**
     * 更新音频
     */
    @RequestMapping("update")
    public Result updateAudio(@RequestBody Audio audio) {
        Audio existingAudio = audioService.getById(audio.getId());
        if (existingAudio != null) {
            existingAudio.setContent(audio.getContent());
            // 如果有新的路径，则更新
            if (audio.getPath() != null && !audio.getPath().isEmpty()) {
                // 删除旧文件
                if (existingAudio.getPath() != null) {
                    File oldFile = new File(existingAudio.getPath());
                    if (oldFile.exists()) {
                        oldFile.delete();
                    }
                }
                existingAudio.setPath(audio.getPath());
            }
            audioService.updateById(existingAudio);
            return Result.success("修改成功");
        }
        return Result.error("音频不存在");
    }
}
