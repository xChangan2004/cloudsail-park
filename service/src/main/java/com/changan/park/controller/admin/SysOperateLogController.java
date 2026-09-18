package com.changan.park.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.changan.common.config.operlog.OperLog;
import com.changan.common.constants.Constants;
import com.changan.park.service.ISysOperateService;
import com.changan.common.domain.R;
import com.changan.common.domain.dto.PageDTO;
import com.changan.model.query.SysOperateLogQuery;
import com.changan.model.vo.SysOperateLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Delete;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/system/operlog")
@RequiredArgsConstructor
@Tag(name = "系统操作日志相关接口")
public class SysOperateLogController {

    private final ISysOperateService sysOperateService;

    @GetMapping("/page")
    @Operation(summary = "分页查询操作日志")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:operlog:list")
    public R<PageDTO<SysOperateLogVO>> queryOperLogPage(SysOperateLogQuery query) {
        return R.ok(sysOperateService.queryOperLogPage(query));
    }

    @PostMapping("/export")
    @Operation(summary = "导出操作日志")
    @OperLog(type = "操作日志", subType = "导出操作日志", recordResult = false)
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:operlog:export")
    public void export(HttpServletResponse response, SysOperateLogQuery query) {
        sysOperateService.export(response, query);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "删除操作日志")
    @OperLog(type = "操作日志", subType = "删除操作日志")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:operlog:delete")
    public R<Void> batchDeleteOperLogByIds(
            @Valid @NotEmpty(message = "请先选择要删除的数据") @RequestBody List<Long> ids) {
        sysOperateService.batchDeleteOperLogByIds(ids);
        return R.ok();
    }
}
