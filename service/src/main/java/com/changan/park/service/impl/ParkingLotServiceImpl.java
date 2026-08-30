package com.changan.park.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changan.common.constants.Constants;
import com.changan.model.vo.AppParkingLotVO;
import com.changan.park.mapper.ParkingLotMapper;
import com.changan.park.service.IParkingLotService;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.CommonStatus;
import com.changan.common.exceptions.BadRequestException;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.model.dto.ParkingLotFormDTO;
import com.changan.model.po.ParkingLot;
import com.changan.model.query.ParkingLotQuery;
import com.changan.park.service.IParkingSpaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParkingLotServiceImpl extends ServiceImpl<ParkingLotMapper, ParkingLot> implements IParkingLotService {

    private final IParkingSpaceService parkingSpaceService;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void saveParkingLot(ParkingLotFormDTO dto) {
        // 1.校验名称是否重复
        boolean exists = lambdaQuery()
                .eq(ParkingLot::getName, dto.getName())
                .exists();
        if (exists) {
            throw new BizIllegalException("停车场名称已存在");
        }
        // 2.拷贝数据到PO
        ParkingLot parkingLot = BeanUtil.copyProperties(dto, ParkingLot.class);
        // 3.保存停车场信息
        save(parkingLot);
        // 4.将停车场位置同步进Redis GEO中
        syncGeo(parkingLot);
    }

    @Override
    public PageDTO<ParkingLot> queryParkingLotPage(ParkingLotQuery query) {
        // 1.分页查询
        Page<ParkingLot> page = lambdaQuery()
                .like(StrUtil.isNotBlank(query.getName()), ParkingLot::getName, query.getName())
                .like(StrUtil.isNotBlank(query.getAddress()), ParkingLot::getAddress, query.getAddress())
                .eq(query.getStatus() != null, ParkingLot::getStatus, query.getStatus())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<ParkingLot> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 2.返回数据
        return PageDTO.of(page);
    }

    @Override
    public ParkingLotFormDTO queryParkingLotById(Long id) {
        // 1.查询停车场
        ParkingLot parkingLot = getById(id);
        if (parkingLot == null) {
            throw new BadRequestException("停车场不存在");
        }
        // 2.拷贝数据到DTO并返回
        return BeanUtil.copyProperties(parkingLot, ParkingLotFormDTO.class);
    }

    @Override
    public void updateParkingLot(ParkingLotFormDTO dto) {
        Long id = dto.getId();
        // 1.查询停车场
        ParkingLot original = getById(id);
        if (original == null) {
            throw new BadRequestException("停车场不存在");
        }
        // 2.校验名称是否重复
        boolean exists = lambdaQuery()
                .eq(ParkingLot::getName, dto.getName())
                .ne(ParkingLot::getId, id)
                .exists();
        if (exists) {
            throw new BizIllegalException("停车场名称已存在");
        }
        // 3.拷贝新的数据到PO
        ParkingLot parkingLot = new ParkingLot();
        BeanUtil.copyProperties(dto, parkingLot);
        // 4.更新停车场信息
        updateById(parkingLot);
        // 5.更新停车场GEO信息
        redisTemplate.opsForGeo().remove(Constants.Lot.LOT_GEO_KEY, id.toString());
        syncGeo(parkingLot);
    }

    @Override
    public void deleteParkingLotById(Long id) {
        // 1.查询停车场
        ParkingLot parkingLot = getById(id);
        if (parkingLot == null) {
            throw new BadRequestException("停车场不存在");
        }
        // 2.只有禁用状态才能删除
        if (parkingLot.getStatus() == CommonStatus.ENABLE) {
            throw new BizIllegalException("启用状态下不可删除");
        }
        // 3.删除停车场
        removeById(id);
        // 4.删除GEO索引
        redisTemplate.opsForGeo().remove(Constants.Lot.LOT_GEO_KEY, id.toString());
    }

    @Override
    public void updateParkingLotStatus(Long id, CommonStatus status) {
        // 1.查询停车场
        ParkingLot exists = getById(id);
        if (exists == null) {
            throw new BadRequestException("停车场不存在");
        }
        // 2.判断是否无效调整
        if (exists.getStatus() == status) {
            return;
        }
        // 3.修改状态
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setId(id);
        parkingLot.setStatus(status);
        updateById(parkingLot);
    }

    @Override
    public List<AppParkingLotVO> listEnabledWithFreeCount() {
        // 1.查询启用中的停车场
        List<ParkingLot> lots = lambdaQuery()
                .eq(ParkingLot::getStatus, CommonStatus.ENABLE)
                .list();
        if (CollUtil.isEmpty(lots)) {
            return Collections.emptyList();
        }
        // 2.组装VO
        return lots.stream().map(lot -> {
            AppParkingLotVO vo = BeanUtil.copyProperties(lot, AppParkingLotVO.class);
            long free = parkingSpaceService.countFreeSpaces(lot.getId());
            vo.setFreeSpaces(free);
            vo.setHasFree(free > 0);
            return vo;
        }).toList();
    }

    private void syncGeo(ParkingLot lot) {
        if (lot.getLongitude() == null || lot.getLatitude() == null) {
            return;
        }
        redisTemplate.opsForGeo()
                .add(Constants.Lot.LOT_GEO_KEY,
                        new RedisGeoCommands.GeoLocation<>(lot.getId().toString(),
                                new Point(lot.getLongitude().doubleValue(), lot.getLatitude().doubleValue())));
    }
}
