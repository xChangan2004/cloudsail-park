package com.changan.park.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.changan.common.enums.CustomerCouponStatus;
import com.changan.model.po.CustomerCoupon;
import com.changan.model.vo.AppCouponTemplateVO;
import com.changan.model.vo.AppCouponVO;

import java.math.BigDecimal;
import java.util.List;

public interface ICustomerCouponService extends IService<CustomerCoupon> {

    void receiveCoupon(Long templateId, Long customerId);

    List<AppCouponVO> queryMyCouponPage(CustomerCouponStatus status, Long customerId);

    List<AppCouponVO> queryUsableCoupons(Long customerId, BigDecimal amount);

    List<AppCouponTemplateVO> queryReceiveableList(Long customerId);
}
