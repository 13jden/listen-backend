package com.example.admin.controller;

import com.example.common.common.Result;
import com.example.common.dto.*;
import com.example.wx.service.AudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/audio")
@RequiredArgsConstructor
public class AudioController {

    private final AudioService audioService;

    @RequestMapping("list")
    public Result getList(@RequestParam int pageNum,
                          @RequestParam int pageSize) {
        return Result.success(audioService.getListAudio(pageNum, pageSize));
    }

    @RequestMapping("search")
    public Result searchAudio(@RequestParam String keyWord,
                              @RequestParam int pageNum,
                              @RequestParam int pageSize) {
        return Result.success(audioService.searchAudio(keyWord, pageNum, pageSize));
    }

    @RequestMapping("uploadFile")
    public Result uploadAudio(@RequestParam("testAudio") MultipartFile testAudio) {
        if (testAudio.isEmpty()) {
            return Result.error("上传文件为空");
        }
        AudioUploadResponse result = audioService.uploadAudio(testAudio);
        return Result.success(result);
    }

    @RequestMapping("addFile")
    public Result addAudio(@RequestBody AudioAddRequest request) {
        AudioDto audio = audioService.save(
                request.getContent(),
                request.getAdminId(),
                request.getFileName(),
                request.getDurationSec()
        );
        if (audio != null) {
            return Result.success(audio);
        }
        return Result.error("添加音频失败");
    }

    @RequestMapping("deleteTempFile")
    public Result deleteTempFile(@RequestParam String fileName) {
        boolean success = audioService.deleteTempFile(fileName);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }

    @RequestMapping("deleteFile")
    public Result deleteAudio(@RequestParam String id) {
        boolean success = audioService.deleteAudio(id);
        return success ? Result.success("删除成功") : Result.error("音频不存在");
    }

    @RequestMapping("update")
    public Result updateAudio(@RequestBody AudioUpdateRequest request) {
        AudioDto audio = audioService.updateAudio(request.getId(), request.getContent(), request.getPath());
        return audio != null ? Result.success("修改成功") : Result.error("音频不存在");
    }
}
