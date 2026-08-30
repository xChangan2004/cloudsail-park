package com.changan.park.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changan.common.config.redisson.annotations.Lock;
import com.changan.common.enums.CommonStatus;
import com.changan.common.enums.CustomerCouponStatus;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.model.po.CouponTemplate;
import com.changan.model.po.CustomerCoupon;
import com.changan.model.vo.AppCouponTemplateVO;
import com.changan.model.vo.AppCouponVO;
import com.changan.park.mapper.CustomerCouponMapper;
import com.changan.park.service.ICouponTemplateService;
import com.changan.park.service.ICustomerCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.changan.common.constants.Constants.Coupon.COUPON_RECEIVE_LOCK_KEY;

@Service
@RequiredArgsConstructor
public class CustomerCouponServiceImpl extends ServiceImpl<CustomerCouponMapper, CustomerCoupon> implements ICustomerCouponService {

    private final ICouponTemplateService couponTemplateService;

    @Override
    @Lock(name = COUPON_RECEIVE_LOCK_KEY)
    @Transactional
    public void receiveCoupon(Long templateId, Long customerId) {
        // 1.获取模板
        CouponTemplate template = couponTemplateService.getById(templateId);
        if (template == null) {
            throw new BizIllegalException("该优惠券不存在");
        }
        if (template.getIssuedCount() >= template.getTotalCount()) {
            throw new BizIllegalException("优惠券已领完");
        }
        // 2.校验模板状态是否为已上架
        if (template.getStatus() != CommonStatus.ENABLE) {
            throw new BizIllegalException("该优惠券不可领取");
        }
        // 3.限领校验，同一用户对同一模板的领取数不能超过perLimit
        Long receiveCount = lambdaQuery()
                .eq(CustomerCoupon::getCustomerId, customerId)
                .eq(CustomerCoupon::getTemplateId, templateId)
                .count();
        if (receiveCount >= template.getPerLimit()) {
            throw new BizIllegalException("已达每人限领数量");
        }
        // 4.扣减优惠券库存
        boolean deducted = couponTemplateService.lambdaUpdate()
                .eq(CouponTemplate::getId, templateId)
                .eq(CouponTemplate::getIssuedCount, template.getIssuedCount())
                .lt(CouponTemplate::getIssuedCount, template.getTotalCount()) // 已发放小于总库存才允许+1
                .setSql("issued_count = issued_count + 1")
                .update();
        if (!deducted) {
            throw new BizIllegalException("手慢了，优惠券已被领完");
        }
        // 5.保存领的券信息
        CustomerCoupon coupon = new CustomerCoupon();
        coupon.setTemplateId(templateId);
        coupon.setCustomerId(customerId);
        coupon.setStatus(CustomerCouponStatus.UNUSED);
        coupon.setExpireTime(LocalDateTime.now().plusDays(template.getValidDays()));
        save(coupon);
    }

    @Override
    public List<AppCouponVO> queryMyCouponPage(CustomerCouponStatus status, Long customerId) {
        // 1.顺手把已到期的UNUSED券流转为EXPIRED
        lambdaUpdate()
                .eq(CustomerCoupon::getCustomerId, customerId)
                .eq(CustomerCoupon::getStatus, CustomerCouponStatus.UNUSED)
                .lt(CustomerCoupon::getExpireTime, LocalDateTime.now())
                .set(CustomerCoupon::getStatus, CustomerCouponStatus.EXPIRED)
                .update();
        // 2.分页查询我的券
        List<CustomerCoupon> coupons = lambdaQuery()
                .eq(CustomerCoupon::getCustomerId, customerId)
                .eq(status != null, CustomerCoupon::getStatus, status)
                .orderByAsc(CustomerCoupon::getStatus)      // 状态升序：0未用→1锁定→2已用→3过期
                .orderByAsc(CustomerCoupon::getExpireTime)  // 同状态下先过期的排前面（营造紧迫感，也方便前端置顶临期券）
                .list();
        if (CollUtil.isEmpty(coupons)) {
            return Collections.emptyList();
        }
        // 3.批量查询模板信息
        Set<Long> templateIds = coupons.stream()
                .map(CustomerCoupon::getTemplateId)
                .collect(Collectors.toSet());
        Map<Long, CouponTemplate> templates = couponTemplateService.listByIds(templateIds)
                .stream()
                .collect(Collectors.toMap(CouponTemplate::getId, Function.identity()));
        // 4.组装VO
        return coupons
                .stream()
                .map(c -> {
                    CouponTemplate t = templates.get(c.getTemplateId());
                    AppCouponVO vo = new AppCouponVO();
                    vo.setId(c.getId());
                    vo.setStatus(c.getStatus());
                    vo.setExpireTime(c.getExpireTime());
                    vo.setReceiveTime(c.getCreateTime());
                    if (t != null) {
                        vo.setName(t.getName());
                        vo.setThreshold(t.getThreshold());
                        vo.setAmount(t.getAmount());
                    }
                    return vo;
                }).toList();
    }

    @Override
    public List<AppCouponVO> queryUsableCoupons(Long customerId, BigDecimal amount) {
        // 一次查询完成三个过滤：我的 + 未用 + 未过期 + 门槛达标
        // 门槛用le比较：订单金额≥threshold才可用（threshold=0即无门槛，天然通过）
        List<CustomerCoupon> coupons = lambdaQuery()
                .eq(CustomerCoupon::getCustomerId, customerId)
                .eq(CustomerCoupon::getStatus, CustomerCouponStatus.UNUSED)
                .gt(CustomerCoupon::getExpireTime, LocalDateTime.now())  // 实时判过期，不依赖惰性流转
                .list();
        if (CollUtil.isEmpty(coupons)) {
            return Collections.emptyList();
        }
        // 批量查询模板信息
        Set<Long> templateIds = coupons.stream()
                .map(CustomerCoupon::getTemplateId)
                .collect(Collectors.toSet());
        Map<Long, CouponTemplate> templates = couponTemplateService.listByIds(templateIds)
                .stream()
                .collect(Collectors.toMap(CouponTemplate::getId, Function.identity()));
        // 返回可用券列表
        return coupons.stream()
                .filter(c -> {
                    CouponTemplate t = templates.get(c.getTemplateId());
                    return t != null && amount.compareTo(t.getThreshold()) >= 0; // 订单额 >= 门槛
                })
                .sorted(Comparator
                        // 抵扣金额降序：省得多的排前面
                        .comparing((CustomerCoupon c) -> templates.get(c.getTemplateId()).getAmount(),
                                Comparator.reverseOrder())
                        // 同抵扣额时，先过期的排前面（临期券优先消耗）
                        .thenComparing(CustomerCoupon::getExpireTime))
                .map(c -> {
                    CouponTemplate t = templates.get(c.getTemplateId());
                    AppCouponVO vo = new AppCouponVO();
                    vo.setId(c.getId());
                    vo.setStatus(c.getStatus());
                    vo.setExpireTime(c.getExpireTime());
                    vo.setReceiveTime(c.getCreateTime());
                    if (t != null) {
                        vo.setName(t.getName());
                        vo.setThreshold(t.getThreshold());
                        vo.setAmount(t.getAmount());
                    }
                    return vo;
                })
                .toList();
    }

    @Override
    public List<AppCouponTemplateVO> queryReceiveableList(Long customerId) {
        // 1.查询所有上架的优惠券
        List<CouponTemplate> templates = couponTemplateService.lambdaQuery()
                .eq(CouponTemplate::getStatus, CommonStatus.ENABLE)
                .list();
        if (CollUtil.isEmpty(templates)) {
            return Collections.emptyList();
        }
        // 2.过滤已领完
        templates = templates.stream()
                .filter(c -> c.getIssuedCount() < c.getTotalCount())
                .toList();
        // 3.根据优惠券模板ID集合批量查询我领取过的优惠券
        List<CustomerCoupon> myCoupons = lambdaQuery()
                .eq(CustomerCoupon::getCustomerId, customerId)
                .in(CustomerCoupon::getTemplateId,
                        templates.stream().map(CouponTemplate::getId).collect(Collectors.toSet()))
                .list();
        // 4.统计每个优惠券被领取的数量
        Map<Long, Long> receivedMap = myCoupons.stream()
                .collect(Collectors.groupingBy(CustomerCoupon::getTemplateId, Collectors.counting()));
        // 5.组装数据返回
        return templates.stream()
                .map(t -> {
                    AppCouponTemplateVO vo = BeanUtil.copyProperties(t, AppCouponTemplateVO.class);
                    vo.setRemainCount(t.getTotalCount() - t.getIssuedCount()); // 剩余可领数
                    Long received = receivedMap.getOrDefault(t.getId(), 0L);
                    vo.setReceived(received.intValue());
                    vo.setCanReceive(received < t.getPerLimit()); // 前端据此置灰"立即领取"按钮
                    return vo;
                }).toList();
    }
}
