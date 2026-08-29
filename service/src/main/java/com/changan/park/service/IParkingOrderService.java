package com.changan.park.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.changan.common.domain.dto.PageDTO;
import com.changan.model.po.EntryExitRecord;
import com.changan.model.po.ParkingOrder;
import com.changan.model.query.ParkingOrderQuery;
import com.changan.model.vo.ParkingOrderDetailVO;
import com.changan.model.vo.ParkingOrderPageVO;

import java.math.BigDecimal;
import java.util.List;

public interface IParkingOrderService extends IService<ParkingOrder> {

    ParkingOrder createExitOrder(EntryExitRecord record, BigDecimal amount);

    PageDTO<ParkingOrderPageVO> queryOrderPage(ParkingOrderQuery query);

    void closeOrder(Long id);

    ParkingOrderDetailVO queryOrderById(Long id);

    List<ParkingOrder> listUnpaidByPlate(String plateNumber);
}
