package com.changan.park.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changan.common.enums.CommonStatus;
import com.changan.common.exceptions.BadRequestException;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.model.dto.FacilityFormDTO;
import com.changan.model.po.ParkingFacility;
import com.changan.model.po.ParkingLot;
import com.changan.model.vo.FacilityItemVO;
import com.changan.park.mapper.ParkingFacilityMapper;
import com.changan.park.mapper.ParkingLotMapper;
import com.changan.park.service.IParkingFacilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParkingFacilityServiceImpl extends ServiceImpl<ParkingFacilityMapper, ParkingFacility> implements IParkingFacilityService {

    private final ParkingLotMapper lotMapper;

    @Override
    public List<ParkingFacility> listFacilities(String code, String name, CommonStatus status) {
        return lambdaQuery()
                .like(StrUtil.isNotBlank(code), ParkingFacility::getCode, code)
                .like(StrUtil.isNotBlank(name), ParkingFacility::getName, name)
                .eq(status != null, ParkingFacility::getStatus, status)
                .orderByAsc(ParkingFacility::getSort)
                .orderByAsc(ParkingFacility::getId)
                .list();
    }

    @Override
    public void saveFacility(FacilityFormDTO dto) {
        // 1.编码查重
        boolean codeExists = lambdaQuery()
                .eq(ParkingFacility::getCode, dto.getCode())
                .exists();
        if (codeExists) {
            throw new BizIllegalException("设施编码已存在");
        }
        // 2.名称查重
        boolean nameExists = lambdaQuery()
                .eq(ParkingFacility::getName, dto.getName())
                .exists();
        if (nameExists) {
            throw new BizIllegalException("设施名称已存在");
        }
        // 3.保存
        ParkingFacility facility = new ParkingFacility();
        facility.setCode(dto.getCode());
        facility.setName(dto.getName());
        facility.setIcon(dto.getIcon());
        facility.setSort(dto.getSort() == null ? 0 : dto.getSort());
        facility.setStatus(dto.getStatus() == null ? CommonStatus.ENABLE : dto.getStatus());
        save(facility);
    }

    @Override
    public void updateFacility(FacilityFormDTO dto) {
        // 1.查询是否存在
        ParkingFacility origin = getById(dto.getId());
        if (origin == null) {
            throw new BizIllegalException("设施不存在");
        }
        // 2.名称查重
        boolean nameExists = lambdaQuery()
                .eq(ParkingFacility::getName, dto.getName())
                .ne(ParkingFacility::getId, dto.getId())
                .exists();
        if (nameExists) {
            throw new BizIllegalException("设施名称已存在");
        }
        // 3.更新设施
        ParkingFacility facility = new ParkingFacility();
        facility.setId(dto.getId());
        facility.setName(dto.getName());
        facility.setIcon(dto.getIcon());
        facility.setSort(dto.getSort() == null ? 0 : dto.getSort());
        if (dto.getStatus() != null) {
            facility.setStatus(dto.getStatus());
        }
        updateById(facility);
    }

    @Override
    public void deleteFacility(Long id) {
        // 1.查询设施
        ParkingFacility facility = getById(id);
        if (facility == null) {
            throw new BadRequestException("设施不存在");
        }
        // 2.被车场引用的设施标签不允许删除
        Long used = lotMapper.selectCount(
                new LambdaQueryWrapper<ParkingLot>()
                        .apply("JSON_CONTAINS(facility_codes, JSON_QUOTE({0}))",
                                facility.getCode()));
        if (used != null && used > 0) {
            throw new BizIllegalException("有" + used + "个车场正在使用【" + facility.getName() + "】，请先在车场里取消后再删除");
        }
        // 3.删除
        removeById(id);
    }

    @Override
    public Map<String, FacilityItemVO> codeItemMap() {
        return lambdaQuery()
                .orderByAsc(ParkingFacility::getSort)
                .list().stream()
                .collect(Collectors.toMap(ParkingFacility::getCode,
                        f -> new FacilityItemVO(f.getCode(), f.getName(), f.getIcon()),
                        (a, b) -> a));
    }

    @Override
    public void validateCodes(List<String> codes) {
        if (CollUtil.isEmpty(codes)) {
            return;
        }
        Set<String> exists = lambdaQuery()
                .in(ParkingFacility::getCode, codes)
                .list().stream()
                .map(ParkingFacility::getCode)
                .collect(Collectors.toSet());
        List<String> invalid = codes.stream().filter(c -> !exists.contains(c)).toList();
        if (!invalid.isEmpty()) {
            throw new BizIllegalException("设施不存在：" + String.join(",", invalid));
        }
    }
}
