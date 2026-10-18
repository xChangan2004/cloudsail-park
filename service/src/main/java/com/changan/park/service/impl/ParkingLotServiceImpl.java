package com.changan.park.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changan.common.constants.Constants;
import com.changan.common.enums.EntryExitStatus;
import com.changan.model.po.EntryExitRecord;
import com.changan.model.po.ParkingFacility;
import com.changan.model.query.AppParkingLotQuery;
import com.changan.model.vo.AppParkingLotVO;
import com.changan.model.vo.FacilityItemVO;
import com.changan.park.mapper.EntryExitRecordMapper;
import com.changan.park.mapper.ParkingFacilityMapper;
import com.changan.park.mapper.ParkingLotMapper;
import com.changan.park.service.IFileService;
import com.changan.park.service.IParkingFacilityService;
import com.changan.park.service.IParkingLotService;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.CommonStatus;
import com.changan.common.exceptions.BadRequestException;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.model.dto.LotFormDTO;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingLotServiceImpl extends ServiceImpl<ParkingLotMapper, ParkingLot> implements IParkingLotService {

    private final RedisTemplate<String, Object> redisTemplate;

    private final EntryExitRecordMapper entryExitRecordMapper;

    private final IParkingFacilityService facilityService;

    private final IFileService fileService;

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
    public void saveParkingLot(LotFormDTO dto) {
        // 1.校验名称是否重复
        boolean exists = lambdaQuery()
                .eq(ParkingLot::getName, dto.getName())
                .exists();
        if (exists) {
            throw new BizIllegalException("停车场名称已存在");
        }
        // 2.拷贝数据到PO
        ParkingLot parkingLot = BeanUtil.copyProperties(dto, ParkingLot.class);
        facilityService.validateCodes(dto.getFacilityCodes());
        // 3.保存停车场信息
        save(parkingLot);
        // 4.将停车场位置同步进Redis GEO中
        syncGeo(parkingLot);
        // 5.标记封面/图集为已使用，防止被临时文件清理任务误删
        List<String> usedUrls = new ArrayList<>();
        if (StrUtil.isNotBlank(dto.getCover())) {
            usedUrls.add(dto.getCover());
        }
        if (CollUtil.isNotEmpty(dto.getImages())) {
            usedUrls.addAll(dto.getImages());
        }
        fileService.markUsed(usedUrls);
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
        List<Long> lotIds = records.stream().map(ParkingLot::getId).toList();
        Map<Long, Long> occupiedMap = countOccupiedByLot(lotIds);
        records.forEach(r -> r.setFreeSpaces(
                (int) freeSpaces(r.getTotalSpaces(), occupiedMap.getOrDefault(r.getId(), 0L))));
        // 3.返回数据
        return PageDTO.of(page);
    }

    @Override
    public ParkingLot queryParkingLotById(Long id) {
        // 1.查询停车场
        ParkingLot lot = getById(id);
        if (lot == null) {
            throw new BadRequestException("停车场不存在");
        }
        // 2.查询可用空位
        lot.setFreeSpaces((int) freeSpaces(lot.getTotalSpaces(), countOccupied(lot.getId())));
        // 3.返回数据
        return lot;
    }

    @Override
    public void updateParkingLot(LotFormDTO dto) {
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
        ParkingLot lot = BeanUtil.copyProperties(dto, ParkingLot.class);
        facilityService.validateCodes(dto.getFacilityCodes());
        // 4.更新停车场信息
        updateById(lot);
        // 5.更新停车场GEO信息
        redisTemplate.opsForGeo().remove(Constants.Lot.LOT_GEO_KEY, id.toString());
        syncGeo(lot);
        // 6.本次提交的图片集合
        Set<String> currentUrls = new HashSet<>();
        if (StrUtil.isNotBlank(dto.getCover())) {
            currentUrls.add(dto.getCover());
        }
        if (CollUtil.isNotEmpty(dto.getImages())) {
            dto.getImages().stream()
                    .filter(StrUtil::isNotBlank)
                    .forEach(currentUrls::add);
        }
        // 7.旧有新无 = 被移出的图，释放引用（USED → TEMP，24 小时后由定时任务真删）
        Set<String> releasedUrls = new HashSet<>();
        if (StrUtil.isNotBlank(original.getCover())) {
            releasedUrls.add(original.getCover());
        }
        if (CollUtil.isNotEmpty(original.getImages())) {
            original.getImages().stream()
                    .filter(StrUtil::isNotBlank)
                    .forEach(releasedUrls::add);
        }
        releasedUrls.removeAll(currentUrls);
        if (CollUtil.isNotEmpty(releasedUrls)) {
            fileService.release(releasedUrls);
        }
        // 8.新图标记为已使用，防止被临时文件清理任务误删
        fileService.markUsed(currentUrls);
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
        // 3.释放封面/图集引用，24 小时后由定时任务回收
        Set<String> urls = new HashSet<>();
        if (StrUtil.isNotBlank(parkingLot.getCover())) {
            urls.add(parkingLot.getCover());
        }
        if (CollUtil.isNotEmpty(parkingLot.getImages())) {
            urls.addAll(parkingLot.getImages());
        }
        if (CollUtil.isNotEmpty(urls)) {
            fileService.release(urls);
        }
        // 4.删除停车场
        removeById(id);
        // 5.删除GEO索引
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
        // 1.GEO_RADIUS：中心点+半径，返回member(lotId)和distance
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

        // 3.批量取在场数
        Map<Long, Long> occupiedMap = countOccupiedByLot(
                lots.stream().map(ParkingLot::getId).toList());

        // 4.组装VO：距离取GEO的计算结果，MySQL为准过滤幽灵ID
        Map<String, FacilityItemVO> facMap = facilityService.codeItemMap();
        List<AppParkingLotVO> voList = lots.stream().map(lot -> {
            AppParkingLotVO vo = buildLotVO(lot, occupiedMap.getOrDefault(lot.getId(), 0L), facMap);
            vo.setDistance(distanceMap.get(lot.getId().toString()));
            return vo;
        }).toList();

        // 5.过滤 + 排序
        voList = filterAndSort(voList, query);

        // 6.切页
        return subPage(voList, query);
    }

    /**
     * 过滤 + 排序
     */
    private List<AppParkingLotVO> filterAndSort(List<AppParkingLotVO> voList, AppParkingLotQuery query) {
        // 1.只看有空位
        if (Boolean.TRUE.equals(query.getOnlyFree())) {
            voList = voList.stream().filter(v -> Boolean.TRUE.equals(v.getHasFree())).toList();
        }
        // 2.排序
        Comparator<AppParkingLotVO> byDistance = Comparator.comparing(
                AppParkingLotVO::getDistance, Comparator.nullsLast(Comparator.naturalOrder())
        );
        // 2.1.获取排序类型
        String sortType = StrUtil.blankToDefault(query.getSortType(), "smart");
        // 2.2.距离排序
        if ("distance".equals(sortType)) {
            return voList.stream().sorted(byDistance).toList();
        }
        // TODO 2.3.价格排序
        // 2.4.智能排序
        return voList.stream()
                .sorted(Comparator.comparing((AppParkingLotVO v) -> !Boolean.TRUE.equals(v.getHasFree()))
                        .thenComparing(byDistance))
                .toList();
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

    private static long freeSpaces(Integer totalSpaces, long occupied) {
        long total = totalSpaces == null ? 0L : totalSpaces;
        return Math.max(0L, total - occupied);
    }

    private long countOccupied(Long lotId) {
        return entryExitRecordMapper.selectCount(new LambdaQueryWrapper<EntryExitRecord>()
                .eq(EntryExitRecord::getLotId, lotId)
                .eq(EntryExitRecord::getStatus, EntryExitStatus.ENTERED));
    }

    private Map<Long, Long> countOccupiedByLot(List<Long> lotIds) {
        if (CollUtil.isEmpty(lotIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = entryExitRecordMapper.selectMaps(
                new QueryWrapper<EntryExitRecord>()
                        .select("lot_id AS lotId", "COUNT(*) AS cnt")
                        .in("lot_id", lotIds)
                        .eq("status", EntryExitStatus.ENTERED)
                        .groupBy("lot_id"));
        Map<Long, Long> occupiedMap = new HashMap<>(rows.size());
        for (Map<String, Object> row : rows) {
            Number lotId = (Number) row.get("lotId");
            Number cnt = (Number) row.get("cnt");
            if (lotId != null && cnt != null) {
                occupiedMap.put(lotId.longValue(), cnt.longValue());
            }
        }
        return occupiedMap;
    }

    /**
     * 组装单个停车场VO
     */
    private AppParkingLotVO buildLotVO(ParkingLot lot, long occupied, Map<String, FacilityItemVO> facMap) {
        AppParkingLotVO vo = BeanUtil.copyProperties(lot, AppParkingLotVO.class);
        long free = freeSpaces(lot.getTotalSpaces(), occupied);
        vo.setFreeSpaces(free);
        vo.setHasFree(free > 0);
        if (lot.getFacilityCodes() != null) {
            vo.setFacilities(lot.getFacilityCodes().stream()
                    .map(facMap::get).filter(Objects::nonNull).toList());
        }
        return vo;
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
