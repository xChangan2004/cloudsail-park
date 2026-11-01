package com.changan.park.controller.app;

import com.changan.common.domain.R;
import com.changan.common.domain.dto.PageDTO;
import com.changan.model.query.AppParkingLotQuery;
import com.changan.model.vo.AppParkingLotDetailVO;
import com.changan.model.vo.AppParkingLotVO;
import com.changan.model.vo.MyParkingStatusVO;
import com.changan.park.service.IEntryExitRecordService;
import com.changan.park.service.IParkingLotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/app/parking")
@RequiredArgsConstructor
@Tag(name = "停车场相关接口")
public class AppParkingController {

    private final IParkingLotService parkingLotService;

    private final IEntryExitRecordService entryExitRecordService;

    @GetMapping("/lots")
    @Operation(summary = "停车场分页列表")
    public R<PageDTO<AppParkingLotVO>> pageLots(@Valid AppParkingLotQuery query) {
        return R.ok(parkingLotService.pageEnabledWithFreeCount(query));
    }

    @GetMapping("/lots/{id}")
    @Operation(summary = "停车场详情")
    public R<AppParkingLotDetailVO> lotDetail(@PathVariable Long id,
                                              @RequestParam(required = false) BigDecimal lng,
                                              @RequestParam(required = false) BigDecimal lat) {
        return R.ok(parkingLotService.getEnabledLotDetail(id, lng, lat));
    }

    @GetMapping("/status")
    @Operation(summary = "我的停车状态")
    public R<List<MyParkingStatusVO>> myStatus() {
        return R.ok(entryExitRecordService.queryMyParkingStatus());
    }
}
