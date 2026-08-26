package com.changan.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.changan.model.po.EntryExitRecord;
import com.changan.model.po.ParkingOrder;

import java.math.BigDecimal;

public interface IParkingOrderService extends IService<ParkingOrder> {

    ParkingOrder createExitOrder(EntryExitRecord record, BigDecimal amount);
}
