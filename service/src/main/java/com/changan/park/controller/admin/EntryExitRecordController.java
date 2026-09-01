package com.changan.park.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.changan.common.config.operlog.OperLog;
import com.changan.common.constants.Constants;
import com.changan.park.service.IEntryExitRecordService;
import com.changan.common.domain.R;
import com.changan.common.domain.dto.PageDTO;
import com.changan.model.dto.EntryFormDTO;
import com.changan.model.dto.ExitFormDTO;
import com.changan.model.query.EntryExitRecordQuery;
import com.changan.model.vo.EntryExitRecordPageVO;
import com.changan.model.vo.ExitBillVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/record")
@RequiredArgsConstructor
@Tag(name = "出入记录相关接口")
public class EntryExitRecordController {

    private final IEntryExitRecordService entryExitRecordService;

    @PostMapping("/entry")
    @Operation(summary = "入场登记")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "parking:record:entry")
    @OperLog(type = "出入记录", subType = "入场登记")
    public R<Void> entry(@RequestBody @Valid EntryFormDTO dto) {
        entryExitRecordService.entry(dto);
        return R.ok();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询出入记录")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "parking:record:list")
    public R<PageDTO<EntryExitRecordPageVO>> queryRecordPage(EntryExitRecordQuery query) {
        return R.ok(entryExitRecordService.queryRecordPage(query));
    }

    @PostMapping("/exit")
    @Operation(summary = "出场登记")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "parking:record:exit")
    @OperLog(type = "出入记录", subType = "出场登记")
    public R<ExitBillVO> exit(@RequestBody @Valid ExitFormDTO dto) {
        return R.ok(entryExitRecordService.exit(dto));
    }
}
