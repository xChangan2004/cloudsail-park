package com.changan.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changan.admin.mapper.*;
import com.changan.admin.service.IEntryExitRecordService;
import com.changan.admin.service.IFeeRuleService;
import com.changan.admin.service.IParkingOrderService;
import com.changan.admin.service.IParkingSpaceService;
import com.changan.admin.strategy.fee.FeeCalculateStrategy;
import com.changan.admin.strategy.fee.FeeStrategyFactory;
import com.changan.common.config.redisson.annotations.Lock;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.EntryExitStatus;
import com.changan.common.enums.EntryType;
import com.changan.common.enums.ParkingSpaceStatus;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.model.dto.EntryFormDTO;
import com.changan.model.dto.ExitFormDTO;
import com.changan.model.po.*;
import com.changan.model.query.EntryExitRecordQuery;
import com.changan.model.vo.EntryExitRecordPageVO;
import com.changan.model.vo.ExitBillVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EntryExitRecordServiceImpl extends ServiceImpl<EntryExitRecordMapper, EntryExitRecord> implements IEntryExitRecordService {

    private final CustomerMapper customerMapper;

    private final CustomerPlateMapper customerPlateMapper;

    private final ParkingLotMapper parkingLotMapper;

    private final ParkingSpaceMapper parkingSpaceMapper;

    private final IFeeRuleService feeRuleService;

    private final FeeStrategyFactory feeStrategyFactory;

    private final IParkingSpaceService parkingSpaceService;

    private final IParkingOrderService parkingOrderService;

    @Override
    @Transactional
    public void entry(EntryFormDTO dto) {
        String upperPlate = dto.getPlateNumber().toUpperCase();
        // 1.根据车牌号反查客户ID
        CustomerPlate plate = customerPlateMapper.selectOne(new LambdaQueryWrapper<CustomerPlate>()
                .select(CustomerPlate::getCustomerId)
                .eq(CustomerPlate::getPlateNumber, upperPlate));
        if (plate == null) {
            throw new BizIllegalException("车牌还未被绑定");
        }
        Long customerId = plate.getCustomerId();
        // 2.检查停车场是否存在
        ParkingLot lot = parkingLotMapper.selectById(dto.getLotId());
        if (lot == null) {
            throw new BizIllegalException("停车场不存在");
        }
        // 3.校验车位是否存在，并且属于该停车场
        ParkingSpace space = parkingSpaceMapper.selectById(dto.getSpaceId());
        if (space == null || !space.getLotId().equals(lot.getId())) {
            throw new BizIllegalException("车位不存在，或车位不属于当前停车场");
        }
        // 3.1.校验车位可用
        if (space.getStatus() != ParkingSpaceStatus.FREE) {
            throw new BizIllegalException("当前车位不可用（已占用或维修中）");
        }
        // 4.该车牌号不能存在正在场内的记录（已入场未出场）
        Long count = lambdaQuery()
                .eq(EntryExitRecord::getPlateNumber, upperPlate)
                .in(EntryExitRecord::getStatus, EntryExitStatus.PENDING_ENTRY, EntryExitStatus.ENTERED)
                .count();
        if (count > 0) {
            throw new BizIllegalException("该车辆已有入场记录，请勿重复申请入场");
        }
        // 5.拷贝数据到实体
        EntryExitRecord record = BeanUtil.copyProperties(dto, EntryExitRecord.class);
        record.setCustomerId(customerId);
        record.setPlateNumber(upperPlate);
        record.setEntryType(EntryType.MANUAL);
        record.setStatus(EntryExitStatus.ENTERED);
        // 6.保存数据
        save(record);
        // 7.更新车位状态
        int rows = parkingSpaceMapper.update(new LambdaUpdateWrapper<ParkingSpace>()
                .eq(ParkingSpace::getId, space.getId())
                .eq(ParkingSpace::getStatus, space.getStatus())
                .set(ParkingSpace::getStatus, ParkingSpaceStatus.OCCUPIED));
        if (rows == 0) {
            throw new BizIllegalException("车位已被抢占或状态已变化，请重试");
        }
        // 8.清除该场地余位缓存
        parkingSpaceService.evictFreeCountCache(dto.getLotId());
    }

    @Override
    public PageDTO<EntryExitRecordPageVO> queryRecordPage(EntryExitRecordQuery query) {
        // 1.分页查询记录
        Page<EntryExitRecord> page = lambdaQuery()
                .eq(query.getLotId() != null, EntryExitRecord::getLotId, query.getLotId())
                .eq(query.getCustomerId() != null, EntryExitRecord::getCustomerId, query.getCustomerId())
                .like(StrUtil.isNotBlank(query.getPlateNumber()), EntryExitRecord::getPlateNumber, query.getPlateNumber())
                .eq(query.getEntryType() != null, EntryExitRecord::getEntryType, query.getEntryType())
                .eq(query.getStatus() != null, EntryExitRecord::getStatus, query.getStatus())
                .ge(query.getBeginTime() != null, EntryExitRecord::getEntryTime, query.getBeginTime())
                .le(query.getEndTime() != null, EntryExitRecord::getEntryTime, query.getEndTime())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<EntryExitRecord> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 2.批量翻译名称：停车场、车位、客户
        Map<Long, String> lotNames = translateLotNames(records);
        Map<Long, String> spaceCodes = translateSpaceCodes(records);
        Map<Long, String> customerNames = translateCustomerNames(records);
        // 3.组装VO
        return PageDTO.of(page, record -> {
            EntryExitRecordPageVO vo = BeanUtil.copyProperties(record, EntryExitRecordPageVO.class);
            vo.setLotName(lotNames.get(record.getLotId()));
            vo.setSpaceCode(spaceCodes.get(record.getSpaceId()));
            vo.setCustomerName(customerNames.get(record.getCustomerId()));
            vo.setDuration(calculateDuration(record));
            return vo;
        });
    }

    @Override
    @Transactional
    @Lock(name = "exit:#{dto.id}")
    public ExitBillVO exit(ExitFormDTO dto) {
        // 1.查询出入记录
        EntryExitRecord record = getById(dto.getId());
        if (record == null) {
            throw new BizIllegalException("出入记录不存在");
        }
        // 2.校验记录状态
        if (record.getStatus() == EntryExitStatus.EXITED) {
            throw new BizIllegalException("该车辆已经办理过出场，请勿重复操作");
        }
        if (record.getStatus() != EntryExitStatus.ENTERED) {
            throw new BizIllegalException("该车辆尚未入场，无法办理出场");
        }
        LocalDateTime exitTime = dto.getExitTime() != null ? dto.getExitTime() : LocalDateTime.now();
        // 3.校验出场时间
        if (exitTime.isBefore(record.getEntryTime())) {
            throw new BizIllegalException("出场时间不能早于入场时间");
        }
        // 4.获取该场地启用的计费规则，计算费用
        FeeRule rule = feeRuleService.getEnableRuleByLotId(record.getLotId());
        FeeCalculateStrategy strategy = feeStrategyFactory.getStrategy(rule.getRuleType());
        BigDecimal amount = strategy.calculate(rule, record.getEntryTime(), exitTime);
        // 5.更新出场时间，及状态为已离场
        boolean updated = lambdaUpdate()
                .eq(EntryExitRecord::getId, record.getId())
                .eq(EntryExitRecord::getStatus, record.getStatus())
                .set(EntryExitRecord::getExitTime, exitTime)
                .set(EntryExitRecord::getStatus, EntryExitStatus.EXITED)
                .update();
        if (!updated) {
            throw new BizIllegalException("车辆状态已发生变化，请刷新后重试");
        }
        // 6.释放车位
        int rows = parkingSpaceMapper.update(new LambdaUpdateWrapper<ParkingSpace>()
                .eq(ParkingSpace::getId, record.getSpaceId())
                .eq(ParkingSpace::getStatus, ParkingSpaceStatus.OCCUPIED)
                .set(ParkingSpace::getStatus, ParkingSpaceStatus.FREE));
        if (rows == 0) {
            throw new BizIllegalException("车位状态异常，请人工核查后重试");
        }
        // 7.生成停车订单
        ParkingOrder order = parkingOrderService.createExitOrder(record, amount);
        // 8.清除该场地余位缓存
        parkingSpaceService.evictFreeCountCache(record.getLotId());
        // 8.组装账单
        ExitBillVO vo = new ExitBillVO();
        vo.setRecordId(record.getId());
        vo.setPlateNumber(record.getPlateNumber());
        vo.setEntryTime(record.getEntryTime());
        vo.setExitTime(exitTime);
        vo.setDuration(Duration.between(record.getEntryTime(), exitTime).toMinutes());
        vo.setRuleName(rule.getRuleName());
        vo.setRuleDesc(strategy.getRuleDesc(rule));
        vo.setAmount(amount);
        vo.setOrderNo(order.getOrderNo());
        return vo;
    }

    /**
     * 批量查询停车场名称，转成 id -> name 映射
     */
    private Map<Long, String> translateLotNames(List<EntryExitRecord> records) {
        Set<Long> ids = records.stream()
                .map(EntryExitRecord::getLotId)
                .collect(Collectors.toSet());
        return parkingLotMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(ParkingLot::getId, ParkingLot::getName));
    }

    /**
     * 批量查询车位编号，转成 id -> spaceCode 映射
     */
    private Map<Long, String> translateSpaceCodes(List<EntryExitRecord> records) {
        Set<Long> ids = records.stream()
                .map(EntryExitRecord::getSpaceId)
                .collect(Collectors.toSet());
        return parkingSpaceMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(ParkingSpace::getId, ParkingSpace::getSpaceCode));
    }

    /**
     * 批量查询客户昵称，转成 id -> nickname 映射
     */
    private Map<Long, String> translateCustomerNames(List<EntryExitRecord> records) {
        Set<Long> ids = records.stream()
                .map(EntryExitRecord::getCustomerId)
                .collect(Collectors.toSet());
        return customerMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getNickname));
    }

    // 计算已出场记录的停车时长（分钟）
    private Long calculateDuration(EntryExitRecord record) {
        if (record.getExitTime() == null || record.getEntryTime() == null) {
            return null;
        }
        return Duration.between(record.getEntryTime(), record.getExitTime()).toMinutes();
    }
}
