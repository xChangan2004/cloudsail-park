package com.changan.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changan.admin.mapper.RefundRecordMapper;
import com.changan.admin.service.IParkingOrderService;
import com.changan.admin.service.IRefundRecordService;
import com.changan.common.domain.dto.PageDTO;
import com.changan.model.po.ParkingOrder;
import com.changan.model.po.RefundRecord;
import com.changan.model.query.RefundRecordQuery;
import com.changan.model.vo.RefundRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IRefundRecordServiceImpl extends ServiceImpl<RefundRecordMapper, RefundRecord> implements IRefundRecordService {

    private final IParkingOrderService parkingOrderService;

    @Override
    public PageDTO<RefundRecordVO> queryRefundRecordPage(RefundRecordQuery query) {
        // 1.按订单号反查订单ID
        Long orderId = null;
        if (StrUtil.isNotBlank(query.getOrderNo())) {
            ParkingOrder order = parkingOrderService.lambdaQuery()
                    .eq(ParkingOrder::getOrderNo, query.getOrderNo())
                    .one();
            if (order == null) {
                // 单号不存在，直接返回空白页
                return PageDTO.empty(0L, 0L);
            }
            orderId = order.getId();
        }
        // 2.分页查询退款记录
        Page<RefundRecord> page = lambdaQuery()
                .eq(orderId != null, RefundRecord::getOrderId, orderId)
                .ge(query.getBeginTime() != null, RefundRecord::getCreateTime, query.getBeginTime())
                .le(query.getEndTime() != null, RefundRecord::getCreateTime, query.getEndTime())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<RefundRecord> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 3.查订单信息，填充数据（orderNo、车牌）
        Set<Long> orderIds = records.stream()
                .map(RefundRecord::getOrderId)
                .collect(Collectors.toSet());
        Map<Long, ParkingOrder> orderMap = parkingOrderService.listByIds(orderIds)
                .stream()
                .collect(Collectors.toMap(ParkingOrder::getId, Function.identity()));
        // 4.组装VO
        return PageDTO.of(page, record -> {
            RefundRecordVO vo = BeanUtil.copyProperties(record, RefundRecordVO.class);
            ParkingOrder order = orderMap.get(record.getOrderId());
            if (order != null) {
                vo.setOrderNo(order.getOrderNo());
                vo.setPlateNumber(order.getPlateNumber());
            }
            return vo;
        });
    }
}
