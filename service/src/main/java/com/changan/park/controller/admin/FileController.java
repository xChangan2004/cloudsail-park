package com.changan.park.controller.admin;

import com.changan.common.domain.R;
import com.changan.model.vo.FileVO;
import com.changan.park.service.IFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "文件上传相关接口")
@RestController
@RequestMapping("/admin/file")
@RequiredArgsConstructor
public class FileController {

    private final IFileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "单文件上传")
    public R<FileVO> upload(@RequestParam("file") MultipartFile file) {
        return R.ok(fileService.upload(file));
    }

    @PostMapping("/upload/batch")
    @Operation(summary = "批量上传")
    public R<List<FileVO>> uploadBatch(@RequestParam("files") List<MultipartFile> files) {
        return R.ok(fileService.uploadBatch(files));
    }
}
