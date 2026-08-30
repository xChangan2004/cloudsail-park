package com.changan.park.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changan.common.enums.CustomerCouponStatus;
import com.changan.model.po.*;
import com.changan.model.query.AppOrderQuery;
import com.changan.model.vo.*;
import com.changan.park.mapper.CustomerMapper;
import com.changan.park.mapper.ParkingLotMapper;
import com.changan.park.mapper.ParkingOrderMapper;
import com.changan.park.service.ICustomerCouponService;
import com.changan.park.service.IParkingOrderService;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.OrderStatus;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.model.query.ParkingOrderQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.changan.common.constants.Constants.Order.ORDER_NO_FORMAT;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingOrderServiceImpl extends ServiceImpl<ParkingOrderMapper, ParkingOrder> implements IParkingOrderService {

    private final ParkingLotMapper parkingLotMapper;

    private final CustomerMapper customerMapper;

    private final ICustomerCouponService customerCouponService;

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
    @Transactional
    public void closeOrder(Long id) {
        // 1.查订单
        ParkingOrder order = getById(id);
        if (order == null) {
            throw new BizIllegalException("订单不存在");
        }
        // 2.只有待支付才可以关闭
        if (order.getStatus() != OrderStatus.UNPAID) {
            throw new BizIllegalException("只有待支付的订单才能关闭");
        }
        // 3.CAS关闭
        boolean updated = lambdaUpdate()
                .eq(ParkingOrder::getId, id)
                .eq(ParkingOrder::getStatus, OrderStatus.UNPAID)
                .set(ParkingOrder::getStatus, OrderStatus.CLOSED)
                .set(ParkingOrder::getDiscount, BigDecimal.ZERO) // 关单清优惠，重新展示为原价
                .update();
        if (!updated) {
            throw new BizIllegalException("订单状态已变化，请刷新后重试");
        }
        // 4.归还优惠券
        boolean released = customerCouponService.lambdaUpdate()
                .eq(CustomerCoupon::getOrderId, id)
                .eq(CustomerCoupon::getStatus, CustomerCouponStatus.LOCKED)
                .set(CustomerCoupon::getStatus, CustomerCouponStatus.UNUSED)
                .set(CustomerCoupon::getOrderId, null)
                .update();
        if (!released) {
            log.debug("订单{}关闭时无锁定优惠券", id);
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

    @Override
    public PageDTO<AppOrderVO> queryMyOrderPage(AppOrderQuery query, Long customerId) {
        // 1.分页查询当前用户的订单
        Page<ParkingOrder> page = lambdaQuery()
                .eq(ParkingOrder::getCustomerId, customerId)
                .eq(query.getStatus() != null, ParkingOrder::getStatus, query.getStatus())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<ParkingOrder> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 2.批量翻译停车场名称
        Map<Long, String> lotNames = translateLotNames(records);
        // 3.组装VO
        return PageDTO.of(page, order -> {
            AppOrderVO vo = BeanUtil.copyProperties(order, AppOrderVO.class);
            vo.setLotName(lotNames.get(order.getLotId()));
            return vo;
        });
    }

    @Override
    public AppOrderDetailVO queryMyOrderDetail(Long id, Long customerId) {
        // 1.查订单
        ParkingOrder order = getById(id);
        if (order == null) {
            throw new BizIllegalException("订单不存在");
        }
        // 2.归属校验：不是自己的订单一律返回“不存在”
        if (!order.getCustomerId().equals(customerId)) {
            throw new BizIllegalException("订单不存在");
        }
        // 3.组装VO
        Map<Long, String> lotNames = translateLotNames(List.of(order));
        AppOrderDetailVO vo = BeanUtil.copyProperties(order, AppOrderDetailVO.class);
        vo.setLotName(lotNames.get(order.getLotId()));
        return vo;
    }

    @Override
    public AppUnpaidCountVO queryMyUnpaidCount(Long customerId) {
        // 一次查询同时拿笔数和总额（列表查询，条数有限不需要SUM聚合SQL）
        List<ParkingOrder> unpaid = lambdaQuery()
                .eq(ParkingOrder::getCustomerId, customerId)
                .eq(ParkingOrder::getStatus, OrderStatus.UNPAID)
                .select(ParkingOrder::getAmount)
                .list();
        AppUnpaidCountVO vo = new AppUnpaidCountVO();
        vo.setCount((long) unpaid.size());
        vo.setTotalAmount(unpaid.stream()
                .map(ParkingOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return vo;
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
