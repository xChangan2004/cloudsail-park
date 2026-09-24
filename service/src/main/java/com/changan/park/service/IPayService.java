package com.changan.park.service;

import com.changan.model.dto.RefundFormDTO;

import java.util.Map;

public interface IPayService {

    String createMyPay(Long orderId, Long couponId, Long customerId);

    boolean handleNotify(Map<String, String> params);

    void cancelOrder(Long orderId);

    void refund(RefundFormDTO dto);
}
