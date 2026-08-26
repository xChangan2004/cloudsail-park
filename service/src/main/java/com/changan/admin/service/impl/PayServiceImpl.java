package com.changan.admin.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.changan.admin.service.IParkingOrderService;
import com.changan.admin.service.IPayService;
import com.changan.admin.service.IPaymentRecordService;
import com.changan.common.config.alipay.AlipayProperties;
import com.changan.common.enums.OrderStatus;
import com.changan.common.enums.PayMethod;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.model.po.ParkingOrder;
import com.changan.model.po.PaymentRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayServiceImpl implements IPayService {

    private final AlipayClient alipayClient;

    private final AlipayProperties alipayProperties;

    private final IParkingOrderService parkingOrderService;

    private final TransactionTemplate transactionTemplate;

    private final IPaymentRecordService paymentRecordService;

    @Override
    public String createPay(String orderNo) {
        // 1.查订单（按订单号精准查询）
        ParkingOrder order = parkingOrderService.lambdaQuery()
                .eq(ParkingOrder::getOrderNo, orderNo)
                .one();
        if (order == null) {
            throw new BizIllegalException("订单不存在");
        }
        // 2.状态校验：只有待支付才能发起
        if (order.getStatus() != OrderStatus.UNPAID) {
            throw new BizIllegalException("当前订单状态不支持支付");
        }
        // 3.组装请求参数
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        // 异步通知：资金事实唯一来源
        request.setNotifyUrl(alipayProperties.getNotifyUrl());
        // 同步回跳
        request.setReturnUrl(alipayProperties.getReturnUrl());
        // 4.业务参数
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", order.getOrderNo()); // 商户订单号
        bizContent.put("total_amount", order.getAmount()); // 金额
        bizContent.put("subject", "停车费-" + order.getPlateNumber()); // 主题
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY"); // 页面支付固定值
        request.setBizContent(bizContent.toJSONString());
        // 5.调用支付宝，返回支付表单HTML
        try {
            return alipayClient.pageExecute(request).getBody();
        } catch (AlipayApiException e) {
            throw new BizIllegalException("创建支付失败：" + e.getMessage());
        }
    }

    @Override
    public boolean handleNotify(Map<String, String> params) {
        // 1.验签
        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(params,
                    alipayProperties.getAlipayPublicKey(),
                    alipayProperties.getCharset(),
                    alipayProperties.getSignType());
            if (!signVerified) {
                log.warn("验签失败，sign={}, sign_type={}, 公钥前50字符={}",
                        params.get("sign"), params.get("sign_type"),
                        alipayProperties.getAlipayPublicKey().substring(0, 50));
                return false;
            }
        } catch (AlipayApiException e) {
            log.error("支付宝回调验签异常", e);
            return false;
        }
        // 2.业务处理
        try {
            transactionTemplate.executeWithoutResult(status -> handlePayNotify(params));
            return true;
        } catch (Exception e) {
            log.error("支付宝回调业务处理异常，params={}", params, e);
            return false;
        }
    }

    private void handlePayNotify(Map<String, String> params) {
        // 1.解析关键字段
        String orderNo = params.get("out_trade_no"); // 商户订单号
        String tradeNo = params.get("trade_no"); // 支付宝交易流水号
        String tradeStatus = params.get("trade_status");// 交易状态
        BigDecimal payAmount = new BigDecimal(params.get("total_amount"));
        // 2.只处理支付成功
        if (!"TRADE_SUCCESS".equals(tradeStatus)) {
            return;
        }
        // 3.查订单
        ParkingOrder order = parkingOrderService.lambdaQuery()
                .eq(ParkingOrder::getOrderNo, orderNo)
                .one();
        if (order == null) {
            throw new BizIllegalException("回调订单不存在：" + orderNo);
        }
        // 4.核对金额（防止篡改，通知金额必须与订单一致）
        if (order.getAmount().compareTo(payAmount) != 0) {
            throw new BizIllegalException("支付金额与订单不符，订单号：" + orderNo);
        }
        // 5.更新订单状态
        boolean updated = parkingOrderService.lambdaUpdate()
                .eq(ParkingOrder::getOrderNo, orderNo)
                .eq(ParkingOrder::getStatus, OrderStatus.UNPAID)
                .set(ParkingOrder::getStatus, OrderStatus.PAID)
                .set(ParkingOrder::getPaid, payAmount)
                .set(ParkingOrder::getPaidTime, LocalDateTime.now())
                .update();
        if (!updated) {
            // 重复通知（订单已是PAID）或订单已关闭：不写流水，直接当成功
            return;
        }
        // 6.写支付流水
        PaymentRecord record = new PaymentRecord();
        record.setOrderId(order.getId());
        record.setTransactionId(tradeNo);
        record.setAmount(payAmount);
        record.setPayMethod(PayMethod.ALIPAY);
        record.setPayTime(LocalDateTime.now());
        record.setCallbackTime(LocalDateTime.now());
        record.setCallbackContent(JSONObject.toJSONString(params));
        paymentRecordService.save(record);
    }
}
