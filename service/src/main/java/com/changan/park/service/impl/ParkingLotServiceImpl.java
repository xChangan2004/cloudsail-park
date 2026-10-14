package com.changan.park.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changan.common.constants.Constants;
import com.changan.common.enums.EntryExitStatus;
import com.changan.common.utils.GeoUtils;
import com.changan.model.po.EntryExitRecord;
import com.changan.model.query.AppParkingLotQuery;
import com.changan.model.vo.AppParkingLotVO;
import com.changan.park.mapper.EntryExitRecordMapper;
import com.changan.park.mapper.ParkingLotMapper;
import com.changan.park.service.IParkingLotService;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.CommonStatus;
import com.changan.common.exceptions.BadRequestException;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.model.dto.ParkingLotFormDTO;
import com.changan.model.po.ParkingLot;
import com.changan.model.query.ParkingLotQuery;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingLotServiceImpl extends ServiceImpl<ParkingLotMapper, ParkingLot> implements IParkingLotService {

    private final RedisTemplate<String, Object> redisTemplate;

    private final EntryExitRecordMapper entryExitRecordMapper;

    /**
     * 启动时全量重建 GEO 索引。
     */
    @PostConstruct
    public void rebuildGeoIndex() {
        try {
            List<ParkingLot> lots = list();
            lots.forEach(this::syncGeo);
            log.info("停车场 GEO 索引重建完成，共 {} 条", lots.size());
        } catch (Exception e) {
            log.error("停车场 GEO 索引构建失败", e);
        }
    }

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
        // 2.查询可用空位
        records.forEach(r -> r.setFreeSpaces((int) countFreeSpaces(r.getId())));
        // 3.返回数据
        return PageDTO.of(page);
    }

    @Override
    public ParkingLot queryParkingLotById(Long id) {
        // 1.查询停车场
        ParkingLot parkingLot = getById(id);
        if (parkingLot == null) {
            throw new BadRequestException("停车场不存在");
        }
        // 2.查询可用空位
        parkingLot.setFreeSpaces((int) countFreeSpaces(id));
        // 3.返回数据
        return parkingLot;
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
    public PageDTO<AppParkingLotVO> pageEnabledWithFreeCount(AppParkingLotQuery query) {
        // 1.带定位+半径：Redis GEO粗筛，拿范围内的ID和距离
        if (query.getLng() != null && query.getLat() != null && query.getRadiusKm() != null) {
            return pageByGeo(query);
        }
        // 2.带定位无半径：MySQL全量（量小）+ 内存算距离排序
        if (query.getLng() != null && query.getLat() != null) {
            return pageByLocation(query);
        }
        // 3.无定位：纯SQL分页
        return pageWithoutLocation(query);
    }

    /**
     * GEO路线：GEORADIUS拿ID+距离 → MySQL补status/name过滤 → 以MySQL为准合并 → 排序切页
     */
    private PageDTO<AppParkingLotVO> pageByGeo(AppParkingLotQuery query) {
        // 1.GEORADIUS：中心点+半径，返回member(lotId)和distance
        GeoResults<RedisGeoCommands.GeoLocation<Object>> results = redisTemplate.opsForGeo()
                .radius(Constants.Lot.LOT_GEO_KEY,
                        new Circle(new Point(query.getLng().doubleValue(), query.getLat().doubleValue()),
                                new Distance(query.getRadiusKm().doubleValue(), Metrics.KILOMETERS)),
                        RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance()
                );
        if (results == null || CollUtil.isEmpty(results.getContent())) {
            return PageDTO.empty(0L, 0L);
        }
        Map<String, Long> distanceMap = new HashMap<>();
        results.getContent().forEach(r ->
                distanceMap.put(r.getContent().getName().toString(),
                        ((Number) r.getDistance().getValue()).longValue()));

        // 2.回MySQL：只查GEO给出的ID中，启用中+name匹配的（GEO不知道status和name）
        List<Long> candidateIds = distanceMap.keySet().stream()
                .map(Long::valueOf).toList();
        List<ParkingLot> lots = lambdaQuery()
                .in(ParkingLot::getId, candidateIds)
                .eq(ParkingLot::getStatus, CommonStatus.ENABLE)
                .like(StrUtil.isNotBlank(query.getName()), ParkingLot::getName, query.getName())
                .list();

        // 3.组装VO：距离取GEO的计算结果，MySQL为准过滤幽灵ID
        List<AppParkingLotVO> voList = lots.stream().map(lot -> {
            AppParkingLotVO vo = buildLotVO(lot);
            vo.setDistance(distanceMap.get(lot.getId().toString()));
            return vo;
        }).sorted(Comparator.comparing(AppParkingLotVO::getDistance)).toList();

        // 4.切页
        return subPage(voList, query);
    }

    /**
     * 定位路线（无radius）：MySQL全量+内存算距离+排序切页（停车场量小时的最优解）
     */
    private PageDTO<AppParkingLotVO> pageByLocation(AppParkingLotQuery query) {
        List<ParkingLot> lots = lambdaQuery()
                .eq(ParkingLot::getStatus, CommonStatus.ENABLE)
                .like(StrUtil.isNotBlank(query.getName()), ParkingLot::getName, query.getName())
                .list();
        if (CollUtil.isEmpty(lots)) {
            return PageDTO.empty(0L, 0L);
        }
        List<AppParkingLotVO> voList = lots.stream().map(lot -> {
            AppParkingLotVO vo = buildLotVO(lot);
            if (lot.getLongitude() != null && lot.getLatitude() != null) {
                vo.setDistance(GeoUtils.calcDistance(
                        query.getLat().doubleValue(), query.getLng().doubleValue(),
                        lot.getLatitude().doubleValue(), lot.getLongitude().doubleValue()));
            }
            return vo;
        }).sorted(Comparator.comparing(AppParkingLotVO::getDistance,
                Comparator.nullsLast(Comparator.naturalOrder()))).toList();
        return subPage(voList, query);
    }

    /**
     * 无定位路线：SQL分页，按create_time倒序
     */
    private PageDTO<AppParkingLotVO> pageWithoutLocation(AppParkingLotQuery query) {
        Page<ParkingLot> page = lambdaQuery()
                .eq(ParkingLot::getStatus, CommonStatus.ENABLE)
                .like(StrUtil.isNotBlank(query.getName()), ParkingLot::getName, query.getName())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<AppParkingLotVO> voList = page.getRecords().stream().map(this::buildLotVO).toList();
        return PageDTO.of(page, voList);
    }

    /**
     * 内存切页（排序后的全量列表按pageNo/pageSize取片）
     */
    private PageDTO<AppParkingLotVO> subPage(List<AppParkingLotVO> sortedList, AppParkingLotQuery query) {
        int total = sortedList.size();
        int from = Math.toIntExact(query.from());
        int to = Math.min(from + query.getPageSize(), total);
        List<AppParkingLotVO> pageList = from >= total
                ? Collections.emptyList() : sortedList.subList(from, to);
        return new PageDTO<>(Long.valueOf(total),
                (long) Math.ceil(total / (double) query.getPageSize()), pageList);
    }

    /**
     * 组装单个停车场VO（余位来自缓存）
     */
    private AppParkingLotVO buildLotVO(ParkingLot lot) {
        AppParkingLotVO vo = BeanUtil.copyProperties(lot, AppParkingLotVO.class);
        long free = countFreeSpaces(lot.getId());
        vo.setFreeSpaces(free);
        vo.setHasFree(free > 0);
        return vo;
    }

    private long countFreeSpaces(Long id) {
        ParkingLot lot = getById(id);
        if (lot == null || lot.getTotalSpaces() == null) {
            return 0;
        }
        Long occupied = entryExitRecordMapper.selectCount(new LambdaQueryWrapper<EntryExitRecord>()
                .eq(EntryExitRecord::getLotId, id)
                .eq(EntryExitRecord::getStatus, EntryExitStatus.ENTERED)
        );
        return Math.max(0, lot.getTotalSpaces() - occupied);
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
