package com.example.admin.controller;

import com.example.common.common.Result;
import com.example.common.dto.*;
import com.example.wx.service.AudioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "音频管理接口")
@RestController
@RequestMapping("/audio")
@RequiredArgsConstructor
public class AudioController {

    private final AudioService audioService;

    @Operation(summary = "获取音频列表", description = "分页获取音频列表")
    @GetMapping("list")
    @PreAuthorize("hasAuthority('Admin')")
    public Result getList(@Parameter(description = "页码") @RequestParam int pageNum,
                          @Parameter(description = "每页数量") @RequestParam int pageSize) {
        return Result.success(audioService.getListAudio(pageNum, pageSize));
    }

    @Operation(summary = "添加音频", description = "添加新音频到数据库")
    @PostMapping("addFile")
    public Result addAudio(@Parameter(description = "音频信息") @RequestBody AudioAddRequest request) {
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

    @Operation(summary = "搜索音频", description = "根据关键词搜索音频")
    @GetMapping("search")
    public Result searchAudio(@Parameter(description = "关键词") @RequestParam String keyWord,
                              @Parameter(description = "页码") @RequestParam int pageNum,
                              @Parameter(description = "每页数量") @RequestParam int pageSize) {
        return Result.success(audioService.searchAudio(keyWord, pageNum, pageSize));
    }

    @Operation(summary = "上传音频文件", description = "上传临时音频文件到OSS")
    @PostMapping("uploadFile")
    public Result uploadAudio(@Parameter(description = "音频文件") @RequestParam("testAudio") MultipartFile testAudio) {
        if (testAudio.isEmpty()) {
            return Result.error("上传文件为空");
        }
        AudioUploadResponse result = audioService.uploadAudio(testAudio);
        return Result.success(result);
    }


    @Operation(summary = "删除临时文件")
    @DeleteMapping("deleteTempFile")
    public Result deleteTempFile(@Parameter(description = "文件名") @RequestParam String fileName) {
        boolean success = audioService.deleteTempFile(fileName);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }

    @Operation(summary = "删除音频")
    @DeleteMapping("deleteFile")
    public Result deleteAudio(@Parameter(description = "音频ID") @RequestParam String id) {
        boolean success = audioService.deleteAudio(id);
        return success ? Result.success("删除成功") : Result.error("音频不存在");
    }

    @Operation(summary = "更新音频信息")
    @PostMapping("update")
    public Result updateAudio(@Parameter(description = "更新信息") @RequestBody AudioUpdateRequest request) {
        AudioDto audio = audioService.updateAudio(request.getId(), request.getContent(), request.getPath());
        return audio != null ? Result.success("修改成功") : Result.error("音频不存在");
    }
}
