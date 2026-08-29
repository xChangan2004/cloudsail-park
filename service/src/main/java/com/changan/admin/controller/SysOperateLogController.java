package com.changan.admin.controller;

import com.changan.admin.service.ISysOperateService;
import com.changan.common.domain.R;
import com.changan.common.domain.dto.PageDTO;
import com.changan.model.query.SysOperateLogQuery;
import com.changan.model.vo.SysOperateLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/operlog")
@RequiredArgsConstructor
@Tag(name = "系统操作日志相关接口")
public class SysOperateLogController {

    private final ISysOperateService sysOperateService;

    @GetMapping("/page")
    @Operation(summary = "分页查询操作日志")
    public R<PageDTO<SysOperateLogVO>> queryOperLogPage(SysOperateLogQuery query) {
        return R.ok(sysOperateService.queryOperLogPage(query));
    }
}
