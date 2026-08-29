package com.changan.park.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changan.park.mapper.PaymentRecordMapper;
import com.changan.park.service.IParkingOrderService;
import com.changan.park.service.IPaymentRecordService;
import com.changan.common.domain.dto.PageDTO;
import com.changan.model.po.ParkingOrder;
import com.changan.model.po.PaymentRecord;
import com.changan.model.query.PaymentRecordQuery;
import com.changan.model.vo.PaymentRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentRecordServiceImpl extends ServiceImpl<PaymentRecordMapper, PaymentRecord> implements IPaymentRecordService {

    private final IParkingOrderService parkingOrderService;

    @Override
    public PageDTO<PaymentRecordVO> queryPaymentRecordPage(PaymentRecordQuery query) {
        // 1.按订单号反查订单ID
        Long orderId = null;
        if (StrUtil.isNotBlank(query.getOrderNo())) {
            ParkingOrder order = parkingOrderService.lambdaQuery()
                    .eq(ParkingOrder::getOrderNo, query.getOrderNo())
                    .one();
            if (order == null) {
                return PageDTO.empty(0L, 0L);
            }
            orderId = order.getId();
        }
        // 2.分页查询
        Page<PaymentRecord> page = lambdaQuery()
                .eq(orderId != null, PaymentRecord::getOrderId, orderId)
                .eq(StrUtil.isNotBlank(query.getTransactionId()), PaymentRecord::getTransactionId, query.getTransactionId())
                .ge(query.getBeginTime() != null, PaymentRecord::getPayTime, query.getBeginTime())
                .le(query.getEndTime() != null, PaymentRecord::getPayTime, query.getEndTime())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<PaymentRecord> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 3.查订单信息，填充数据
        Set<Long> orderIds = records.stream()
                .map(PaymentRecord::getOrderId)
                .collect(Collectors.toSet());
        Map<Long, ParkingOrder> orderMap = parkingOrderService.listByIds(orderIds)
                .stream()
                .collect(Collectors.toMap(ParkingOrder::getId, Function.identity()));
        // 4.组装VO
        return PageDTO.of(page, record -> {
            PaymentRecordVO vo = BeanUtil.copyProperties(record, PaymentRecordVO.class);
            ParkingOrder order = orderMap.get(record.getOrderId());
            if (order != null) {
                vo.setOrderNo(order.getOrderNo());
                vo.setPlateNumber(order.getPlateNumber());
            }
            return vo;
        });
    }
}
