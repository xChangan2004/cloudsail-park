package com.changan.park.controller.app;

import com.changan.common.domain.R;
import com.changan.model.vo.AppRecordDetailVO;
import com.changan.park.service.IEntryExitRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/record")
@RequiredArgsConstructor
@Tag(name = "出入记录相关接口")
public class AppRecordController {

    private final IEntryExitRecordService entryExitRecordService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询出入记录")
    public R<AppRecordDetailVO> queryRecordById(@PathVariable Long id) {
        return R.ok(entryExitRecordService.queryRecordById(id));
    }
}
