package com.changan.admin.service;

import java.util.Map;

public interface IPayService {

    String createPay(String orderNo);

    boolean handleNotify(Map<String, String> params);
}
