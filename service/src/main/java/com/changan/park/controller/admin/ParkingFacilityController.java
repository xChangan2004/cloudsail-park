package com.changan.park.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.changan.common.config.operlog.OperLog;
import com.changan.common.constants.Constants;
import com.changan.common.domain.R;
import com.changan.common.enums.CommonStatus;
import com.changan.model.dto.FacilityFormDTO;
import com.changan.model.po.ParkingFacility;
import com.changan.park.service.IParkingFacilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "停车场服务设施相关接口")
@RestController
@RequestMapping("/admin/parking/facility")
@RequiredArgsConstructor
public class ParkingFacilityController {

    private final IParkingFacilityService facilityService;

    @GetMapping("/list")
    @Operation(summary = "服务设施列表")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "parking:facility:list")
    public R<List<ParkingFacility>> list(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) CommonStatus status) {
        return R.ok(facilityService.listFacilities(code, name, status));
    }

    @PostMapping
    @Operation(summary = "新增服务设施")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "parking:facility:add")
    @OperLog(type = "服务设施", subType = "新增服务设施")
    public R<Void> add(@Valid @RequestBody FacilityFormDTO dto) {
        facilityService.saveFacility(dto);
        return R.ok();
    }

    @PutMapping
    @Operation(summary = "修改服务设施")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "parking:facility:edit")
    @OperLog(type = "服务设施", subType = "修改服务设施")
    public R<Void> edit(@Valid @RequestBody FacilityFormDTO dto) {
        facilityService.updateFacility(dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除服务设施")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "parking:facility:delete")
    @OperLog(type = "服务设施", subType = "删除服务设施")
    public R<Void> delete(@PathVariable @NotNull Long id) {
        facilityService.deleteFacility(id);
        return R.ok();
    }
}
