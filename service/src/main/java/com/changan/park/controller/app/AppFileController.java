package com.changan.park.controller.app;

import com.changan.common.constants.Constants;
import com.changan.common.domain.R;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.common.utils.RateLimitUtil;
import com.changan.common.utils.StpKit;
import com.changan.model.vo.FileVO;
import com.changan.park.service.IFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文件上传相关接口（用户端）")
@RestController
@RequestMapping("/app/file")
@RequiredArgsConstructor
public class AppFileController {

    private final IFileService fileService;

    private final RateLimitUtil rateLimitUtil;

    @PostMapping("/upload")
    @Operation(summary = "单文件上传，返回临时文件地址，需业务保存时引用才会转正")
    public R<FileVO> upload(@RequestParam("file") MultipartFile file) {
        String key = Constants.App.UPLOAD_LIMIT_KEY_PREFIX + StpKit.APP.getLoginIdAsLong();
        boolean allowed = rateLimitUtil.tryAcquire(
                key,
                Constants.App.UPLOAD_LIMIT_COUNT,
                Constants.App.UPLOAD_LIMIT_WINDOW
        );
        if (!allowed) {
            throw new BizIllegalException("上传太频繁，请稍后再试");
        }
        return R.ok(fileService.upload(file));
    }
}
