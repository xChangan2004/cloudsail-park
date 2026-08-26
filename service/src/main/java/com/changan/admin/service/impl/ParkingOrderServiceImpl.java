package com.changan.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changan.admin.mapper.ParkingOrderMapper;
import com.changan.admin.service.IParkingOrderService;
import com.changan.common.enums.OrderStatus;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.model.po.EntryExitRecord;
import com.changan.model.po.ParkingOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ThreadLocalRandom;

import static com.changan.common.constants.Constants.Order.ORDER_NO_FORMAT;

@Service
@RequiredArgsConstructor
public class ParkingOrderServiceImpl extends ServiceImpl<ParkingOrderMapper, ParkingOrder> implements IParkingOrderService {

    @Override
    public ParkingOrder createExitOrder(EntryExitRecord record, BigDecimal amount) {
        ParkingOrder order = new ParkingOrder();
        order.setOrderNo(generateOrderNo());
        order.setLotId(record.getLotId());
        order.setCustomerId(record.getCustomerId());
        order.setRecordId(record.getId());
        order.setPlateNumber(record.getPlateNumber());
        order.setAmount(amount);
        order.setDiscount(BigDecimal.ZERO);
        order.setPaid(BigDecimal.ZERO);
        order.setStatus(OrderStatus.UNPAID);
        save(order);
        return order;
    }

    /**
     * CP + yyyyMMddHHmmss + 6位随机数 0~999999
     */
    private String generateOrderNo() {
        return "CP" + LocalDateTime.now(ZoneId.of("Asia/Shanghai")).format(ORDER_NO_FORMAT)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
    }
}
