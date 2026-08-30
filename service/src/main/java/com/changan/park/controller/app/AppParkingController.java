package com.changan.park.controller.app;

import com.changan.common.domain.R;
import com.changan.common.utils.StpKit;
import com.changan.model.vo.AppParkingLotVO;
import com.changan.model.vo.MyParkingStatusVO;
import com.changan.park.service.IEntryExitRecordService;
import com.changan.park.service.IParkingLotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/app/parking")
@RequiredArgsConstructor
@Tag(name = "停车场相关接口")
public class AppParkingController {

    private final IParkingLotService parkingLotService;

    private final IEntryExitRecordService entryExitRecordService;

    @GetMapping("/lots")
    @Operation(summary = "停车场列表（含实时余位）")
    public R<List<AppParkingLotVO>> listLots() {
        return R.ok(parkingLotService.listEnabledWithFreeCount());
    }

    @GetMapping("/status")
    @Operation(summary = "我的停车状态")
    public R<MyParkingStatusVO> myStatus() {
        return R.ok(entryExitRecordService.queryMyParkingStatus());
    }
}
