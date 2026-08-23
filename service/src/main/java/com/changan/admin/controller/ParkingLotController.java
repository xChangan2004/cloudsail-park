package com.changan.admin.controller;

import com.changan.admin.service.IParkingLotService;
import com.changan.common.domain.R;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.CommonStatus;
import com.changan.model.dto.ParkingLotFormDTO;
import com.changan.model.po.ParkingLot;
import com.changan.model.query.ParkingLotQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "停车场相关接口")
@RestController
@RequestMapping("/parking/lot")
@RequiredArgsConstructor
public class ParkingLotController {

    private final IParkingLotService parkingLotService;

    @PostMapping
    @Operation(summary = "新增停车场")
//    @SaCheckPermission("parking:lot:add")
    public R<Void> saveParkingLot(@Valid @RequestBody ParkingLotFormDTO dto) {
        parkingLotService.saveParkingLot(dto);
        return R.ok();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询停车场列表")
//    @SaCheckPermission("parking:lot:page")
    public R<PageDTO<ParkingLot>> queryParkingLotPage(ParkingLotQuery query) {
        return R.ok(parkingLotService.queryParkingLotPage(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询停车场详情")
//    @SaCheckPermission("parking:lot:edit")
    public R<ParkingLotFormDTO> queryParkingLotById(@PathVariable Long id) {
        return R.ok(parkingLotService.queryParkingLotById(id));
    }

    @PutMapping
    @Operation(summary = "修改停车场")
//    @SaCheckPermission("parking:lot:edit")
    public R<Void> updateParkingLot(@Valid @RequestBody ParkingLotFormDTO dto) {
        parkingLotService.updateParkingLot(dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除停车场")
//    @SaCheckPermission("parking:lot:delete")
    public R<Void> deleteParkingLotById(@PathVariable Long id) {
        parkingLotService.deleteParkingLotById(id);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "切换停车场状态")
//    @SaCheckPermission("parking:lot:edit")
    public R<Void> updateParkingLotStatus(
            @PathVariable @NotNull(message = "停车场id不能为空") Long id,
            @RequestParam @NotNull(message = "状态不能为空") CommonStatus status) {
        parkingLotService.updateParkingLotStatus(id, status);
        return R.ok();
    }
}
