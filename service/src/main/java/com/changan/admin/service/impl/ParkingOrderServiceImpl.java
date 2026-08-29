package com.changan.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changan.admin.mapper.CustomerMapper;
import com.changan.admin.mapper.ParkingLotMapper;
import com.changan.admin.mapper.ParkingOrderMapper;
import com.changan.admin.service.IParkingOrderService;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.OrderStatus;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.model.po.Customer;
import com.changan.model.po.EntryExitRecord;
import com.changan.model.po.ParkingLot;
import com.changan.model.po.ParkingOrder;
import com.changan.model.query.ParkingOrderQuery;
import com.changan.model.vo.ParkingOrderDetailVO;
import com.changan.model.vo.ParkingOrderPageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.changan.common.constants.Constants.Order.ORDER_NO_FORMAT;

@Service
@RequiredArgsConstructor
public class ParkingOrderServiceImpl extends ServiceImpl<ParkingOrderMapper, ParkingOrder> implements IParkingOrderService {

    private final ParkingLotMapper parkingLotMapper;

    private final CustomerMapper customerMapper;

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

    @Override
    public PageDTO<ParkingOrderPageVO> queryOrderPage(ParkingOrderQuery query) {
        // 1.分页查订单
        Page<ParkingOrder> page = lambdaQuery()
                .eq(StrUtil.isNotBlank(query.getOrderNo()), ParkingOrder::getOrderNo, query.getOrderNo())
                .eq(query.getLotId() != null, ParkingOrder::getLotId, query.getLotId())
                .like(StrUtil.isNotBlank(query.getPlateNumber()), ParkingOrder::getPlateNumber, query.getPlateNumber())
                .eq(query.getStatus() != null, ParkingOrder::getStatus, query.getStatus())
                .ge(query.getBeginTime() != null, ParkingOrder::getCreateTime, query.getBeginTime())
                .le(query.getEndTime() != null, ParkingOrder::getCreateTime, query.getEndTime())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<ParkingOrder> orders = page.getRecords();
        if (CollUtil.isEmpty(orders)) {
            return PageDTO.empty(page);
        }
        // 2.批量翻译名称：停车场、客户
        Map<Long, String> lotNames = translateLotNames(orders);
        Map<Long, String> customerNames = translateCustomerNames(orders);
        // 3.组装VO
        return PageDTO.of(page, order -> {
            ParkingOrderPageVO vo = BeanUtil.copyProperties(order, ParkingOrderPageVO.class);
            vo.setLotName(lotNames.get(order.getLotId()));
            vo.setCustomerName(customerNames.get(order.getCustomerId()));
            return vo;
        });
    }

    @Override
    public void closeOrder(Long id) {
        // 1.查订单
        ParkingOrder order = getById(id);
        if (order == null) {
            throw new BizIllegalException("订单不存在");
        }
        // 2.状态校验：只有待支付才可以关闭
        if (order.getStatus() != OrderStatus.UNPAID) {
            throw new BizIllegalException("只有待支付的订单才能关闭");
        }
        // 3.CAS关闭
        boolean updated = lambdaUpdate()
                .eq(ParkingOrder::getId, id)
                .eq(ParkingOrder::getStatus, OrderStatus.UNPAID)
                .set(ParkingOrder::getStatus, OrderStatus.CLOSED)
                .update();
        if (!updated) {
            throw new BizIllegalException("订单状态已变化，请刷新后重试");
        }
    }

    @Override
    public ParkingOrderDetailVO queryOrderById(Long id) {
        // 1.查订单
        ParkingOrder order = getById(id);
        if (order == null) {
            throw new BizIllegalException("订单不存在");
        }
        // 2.翻译名称
        Map<Long, String> lotNames = translateLotNames(List.of(order));
        Map<Long, String> customerNames = translateCustomerNames(List.of(order));
        // 3.组装VO
        ParkingOrderDetailVO vo = BeanUtil.copyProperties(order, ParkingOrderDetailVO.class);
        vo.setLotName(lotNames.get(order.getLotId()));
        vo.setCustomerName(customerNames.get(order.getCustomerId()));
        return vo;
    }

    @Override
    public List<ParkingOrder> listUnpaidByPlate(String plateNumber) {
        return lambdaQuery()
                .eq(ParkingOrder::getPlateNumber, plateNumber)
                .eq(ParkingOrder::getStatus, OrderStatus.UNPAID)
                .list();
    }

    /**
     * 批量查询停车场名称，转成 id -> name 映射
     */
    private Map<Long, String> translateLotNames(List<ParkingOrder> records) {
        Set<Long> ids = records.stream()
                .map(ParkingOrder::getLotId)
                .collect(Collectors.toSet());
        return parkingLotMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(ParkingLot::getId, ParkingLot::getName));
    }

    /**
     * 批量查询客户昵称，转成 id -> nickname 映射
     */
    private Map<Long, String> translateCustomerNames(List<ParkingOrder> records) {
        Set<Long> ids = records.stream()
                .map(ParkingOrder::getCustomerId)
                .collect(Collectors.toSet());
        return customerMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getNickname));
    }

    /**
     * CP + yyyyMMddHHmmss + 6位随机数 0~999999
     */
    private String generateOrderNo() {
        return "CP" + LocalDateTime.now(ZoneId.of("Asia/Shanghai")).format(ORDER_NO_FORMAT)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
    }
}
