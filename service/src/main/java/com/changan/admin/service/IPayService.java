package com.changan.admin.service;

import com.changan.model.dto.RefundFormDTO;

import java.util.Map;

public interface IPayService {

    String createPay(String orderNo);

    boolean handleNotify(Map<String, String> params);

    void refund(RefundFormDTO dto);
}
