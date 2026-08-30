package com.changan.park.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changan.park.mapper.ParkingLotMapper;
import com.changan.park.mapper.ParkingSpaceMapper;
import com.changan.park.service.IParkingSpaceService;
import com.changan.common.constants.Constants;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.ParkingSpaceStatus;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.model.po.ParkingLot;
import com.changan.model.po.ParkingSpace;
import com.changan.model.query.ParkingSpaceQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParkingSpaceServiceImpl extends ServiceImpl<ParkingSpaceMapper, ParkingSpace> implements IParkingSpaceService {

    private final ParkingLotMapper parkingLotMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public PageDTO<ParkingSpace> queryParkingSpacePage(ParkingSpaceQuery query) {
        // 1.查询车位
        Page<ParkingSpace> page = lambdaQuery()
                .eq(query.getLotId() != null, ParkingSpace::getLotId, query.getLotId())
                .like(StrUtil.isNotBlank(query.getSpaceCode()), ParkingSpace::getSpaceCode, query.getSpaceCode())
                .eq(query.getSpaceType() != null, ParkingSpace::getSpaceType, query.getSpaceType())
                .eq(query.getStatus() != null, ParkingSpace::getStatus, query.getStatus())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        // 2.返回数据
        return PageDTO.of(page);
    }

    @Override
    @Transactional
    public void saveParkingSpace(ParkingSpace parkingSpace) {
        // 1.查询停车场
        Long lotId = parkingSpace.getLotId();
        ParkingLot parkingLot = parkingLotMapper.selectById(lotId);
        if (parkingLot == null) {
            throw new BizIllegalException("关联的停车场不存在");
        }
        // 2.检查当前停车场下车位编号是否相同
        boolean exists = lambdaQuery()
                .eq(ParkingSpace::getLotId, lotId)
                .eq(ParkingSpace::getSpaceCode, parkingSpace.getSpaceCode())
                .exists();
        if (exists) {
            throw new BizIllegalException("关联的停车场存在相同车位编号");
        }
        // 3.保存车位信息
        save(parkingSpace);
        // 4.更新停车场总车位数
        parkingLotMapper.update(null,
                new LambdaUpdateWrapper<ParkingLot>()
                        .eq(ParkingLot::getId, lotId)
                        .setSql("total_spaces = total_spaces + 1")
        );
    }

    @Override
    @Transactional
    public void saveParkingSpaceBatch(List<ParkingSpace> parkingSpaces) {
        // 1.校验所有车位是否属于同一停车场
        Long lotId = parkingSpaces.get(0).getLotId();
        boolean sameLot = parkingSpaces.stream()
                .allMatch(ps -> lotId.equals(ps.getLotId()));
        if (!sameLot) {
            throw new BizIllegalException("批量新增的车位必须属于同一停车场");
        }

        // 2.查询停车场
        ParkingLot parkingLot = parkingLotMapper.selectById(lotId);
        if (parkingLot == null) {
            throw new BizIllegalException("关联的停车场不存在");
        }

        // 3.检查批量内部是否有重复编号
        Set<String> spaceCodes = parkingSpaces.stream()
                .map(ParkingSpace::getSpaceCode)
                .collect(Collectors.toSet());
        if (spaceCodes.size() != parkingSpaces.size()) {
            throw new BizIllegalException("批量新增中存在重复的车位编号");
        }

        // 4.检查当前停车场下车位编号是否已存在
        boolean exists = lambdaQuery()
                .eq(ParkingSpace::getLotId, lotId)
                .in(ParkingSpace::getSpaceCode, spaceCodes)
                .exists();
        if (exists) {
            throw new BizIllegalException("关联的停车场存在相同车位编号");
        }

        // 5.批量保存车位信息
        saveBatch(parkingSpaces);

        // 6.更新停车场总车位数
        parkingLotMapper.update(
                new LambdaUpdateWrapper<ParkingLot>()
                        .eq(ParkingLot::getId, lotId)
                        .setSql("total_spaces = total_spaces + " + parkingSpaces.size())
        );
    }

    @Override
    public ParkingSpace getParkingSpace(Long id) {
        return getById(id);
    }

    @Override
    @Transactional
    public void deleteParkingSpace(Long id) {
        // 1.查询车位信息
        ParkingSpace space = getById(id);
        if (space == null) {
            throw new BizIllegalException("车位不存在");
        }
        // 1.1.占用中的车位不能删除
        if (space.getStatus() == ParkingSpaceStatus.OCCUPIED) {
            throw new BizIllegalException("占用中的车位不能删除");
        }
        // 2.删除车位信息
        removeById(id);
        // 3.移除停车场将相关车位减少
        parkingLotMapper.update(new LambdaUpdateWrapper<ParkingLot>()
                .eq(ParkingLot::getId, space.getLotId())
                .setSql("total_spaces = total_spaces - 1"));
    }

    @Override
    public void updateParkingSpaceStatus(Long id, ParkingSpaceStatus status) {
        // 1.查询车位信息
        ParkingSpace exists = getById(id);
        if (exists == null) {
            throw new BizIllegalException("车位不存在");
        }
        // 2.判断是否无效调整
        if (exists.getStatus() == status) {
            return;
        }
        // 占用中的车位不可以手动进行更改
        if (exists.getStatus() == ParkingSpaceStatus.OCCUPIED) {
            throw new BizIllegalException("占用中的车位不能手动切换状态");
        }
        // 也不能手动调整状态为占用
        if (status == ParkingSpaceStatus.OCCUPIED) {
            throw new BizIllegalException("不能手动切换状态为“占用”");
        }
        // 3.调整状态
        ParkingSpace space = new ParkingSpace();
        space.setId(id);
        space.setStatus(status);
        updateById(space);
        // 4.清除该场地余位缓存
        evictFreeCountCache(exists.getLotId());
    }

    @Override
    public long countFreeSpaces(Long lotId) {
        String key = Constants.Spaces.FREE_COUNT_KEY + lotId;
        // 1.检查Redis中是否有缓存的余位
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return Long.parseLong(cached.toString());
        }
        // 2.查数据库
        Long count = lambdaQuery()
                .eq(ParkingSpace::getLotId, lotId)
                .eq(ParkingSpace::getStatus, ParkingSpaceStatus.FREE)
                .count();
        // 3.缓存到Redis中，60秒后自动作废
        redisTemplate.opsForValue().set(key, count, Constants.Spaces.FREE_COUNT_TTL, TimeUnit.SECONDS);
        return count;
    }

    @Override
    public void evictFreeCountCache(Long lotId) {
        redisTemplate.delete(Constants.Spaces.FREE_COUNT_KEY + lotId);
    }
}
