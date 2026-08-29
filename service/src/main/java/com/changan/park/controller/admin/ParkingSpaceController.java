package com.changan.park.controller.admin;

import com.changan.park.service.IParkingSpaceService;
import com.changan.common.domain.R;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.ParkingSpaceStatus;
import com.changan.model.po.ParkingSpace;
import com.changan.model.query.ParkingSpaceQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "停车场车位相关接口")
@RestController
@RequestMapping("/admin/parking/space")
@RequiredArgsConstructor
public class ParkingSpaceController {

    private final IParkingSpaceService parkingSpaceService;

    @GetMapping("/page")
    @Operation(summary = "分页查询车位列表")
    public R<PageDTO<ParkingSpace>> queryParkingSpacePage(@Valid ParkingSpaceQuery query) {
        return R.ok(parkingSpaceService.queryParkingSpacePage(query));
    }

    @PostMapping
    @Operation(summary = "新增单个车位")
    public R<Void> saveParkingSpace(@Valid @RequestBody ParkingSpace parkingSpace) {
        parkingSpaceService.saveParkingSpace(parkingSpace);
        return R.ok();
    }

    @PostMapping("/batch")
    @Operation(summary = "批量新增车位")
    public R<Void> saveParkingSpaceBatch(
            @RequestBody @NotNull(message = "车位信息不能为空") List<@Valid ParkingSpace> parkingSpaces) {
        parkingSpaceService.saveParkingSpaceBatch(parkingSpaces);
        return R.ok();
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询车位详情")
    public R<ParkingSpace> getParkingSpace(@PathVariable @NotNull(message = "车位id不能为空") Long id) {
        return R.ok(parkingSpaceService.getParkingSpace(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除车位")
    public R<Void> deleteParkingSpace(@PathVariable @NotNull(message = "车位id不能为空") Long id) {
        parkingSpaceService.deleteParkingSpace(id);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "切换车位状态")
    public R<Void> updateParkingSpaceStatus(@PathVariable @NotNull(message = "车位id不能为空") Long id,
                                            @RequestParam @NotNull(message = "状态不能为空") ParkingSpaceStatus status) {
        parkingSpaceService.updateParkingSpaceStatus(id, status);
        return R.ok();
    }
}
