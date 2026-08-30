package com.changan.park.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.changan.common.config.alipay.AlipayProperties;
import com.changan.common.config.redisson.annotations.Lock;
import com.changan.common.enums.OrderStatus;
import com.changan.common.enums.PayMethod;
import com.changan.common.enums.RefundStatus;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.model.dto.RefundFormDTO;
import com.changan.model.po.ParkingOrder;
import com.changan.model.po.PaymentRecord;
import com.changan.model.po.RefundRecord;
import com.changan.park.service.IParkingOrderService;
import com.changan.park.service.IPayService;
import com.changan.park.service.IPaymentRecordService;
import com.changan.park.service.IRefundRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayServiceImpl implements IPayService {

    private final AlipayClient alipayClient;

    private final AlipayProperties alipayProperties;

    private final IParkingOrderService parkingOrderService;

    private final TransactionTemplate transactionTemplate;

    private final IPaymentRecordService paymentRecordService;

    private final IRefundRecordService refundRecordService;

    @Override
    public String createMyPay(Long orderId, Long customerId) {
        // 1.查订单并校验归属
        ParkingOrder order = parkingOrderService.getById(orderId);
        if (order == null) {
            throw new BizIllegalException("订单不存在");
        }
        if (!order.getCustomerId().equals(customerId)) {
            throw new BizIllegalException("该订单不属于您");
        }
        // 2.状态校验
        if (order.getStatus() != OrderStatus.UNPAID) {
            throw new BizIllegalException("当前订单状态不支持支付");
        }
        // 3.发起H5支付
        AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
        // 3.1.设置异步通知
        request.setNotifyUrl(alipayProperties.getNotifyUrl());
        // 3.2.设置同步回跳
        request.setReturnUrl(alipayProperties.getReturnUrl());
        // 3.3.设置业务参数
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", order.getOrderNo());
        bizContent.put("total_amount", order.getAmount());
        bizContent.put("subject", "停车费-" + order.getPlateNumber());
        bizContent.put("product_code", "QUICK_WAP_WAY");
        request.setBizContent(bizContent.toJSONString());
        // 4.调用支付宝，返回支付表单HTML
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

    @Override
    @Lock(name = "refund:#{dto.orderNo}")
    public void refund(RefundFormDTO dto) {
        // 1.查订单
        ParkingOrder order = parkingOrderService.lambdaQuery()
                .eq(ParkingOrder::getOrderNo, dto.getOrderNo())
                .one();
        if (order == null) {
            throw new BizIllegalException("订单不存在");
        }
        // 2.校验状态（只有已支付才能退）
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BizIllegalException("只有已支付的订单才能退款");
        }
        // 3.查支付流水
        PaymentRecord payment = paymentRecordService.lambdaQuery()
                .eq(PaymentRecord::getOrderId, order.getId())
                .one();
        if (payment == null) {
            throw new BizIllegalException("支付流水不存在，无法退款");
        }
        // 4.生成退款单号
        String refundNo = generateRefundNo();
        // 5.调用支付宝退款（同步）
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", order.getOrderNo()); // 原商户订单号
        bizContent.put("refund_amount", order.getPaid());   // 退实付金额
        bizContent.put("out_request_no", refundNo);         // 幂等键
        bizContent.put("refund_reason", dto.getReason());   // 退款原因
        request.setBizContent(bizContent.toJSONString());

        AlipayTradeRefundResponse response;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            throw new BizIllegalException("调用支付宝退款失败：" + e.getMessage());
        }
        if (!response.isSuccess()) {
            throw new BizIllegalException("退款失败：" + response.getSubMsg());
        }
        // 5.支付宝退款成功，调整订单状态
        String tradeNo = response.getTradeNo(); // 支付宝交易号（第三方交易流水号）
        transactionTemplate.executeWithoutResult(status -> {
            boolean updated = parkingOrderService.lambdaUpdate()
                    .eq(ParkingOrder::getId, order.getId())
                    .eq(ParkingOrder::getStatus, OrderStatus.PAID)
                    .set(ParkingOrder::getStatus, OrderStatus.REFUNDED)
                    .update();
            if (!updated) {
                throw new BizIllegalException("订单状态已变化，请刷新后重试");
            }
            // 6.写入退款记录
            RefundRecord refund = new RefundRecord();
            refund.setOrderId(order.getId());
            refund.setPaymentId(payment.getId());
            refund.setRefundNo(refundNo);
            refund.setTransactionId(tradeNo);
            refund.setAmount(order.getPaid());
            refund.setReason(dto.getReason());
            refund.setStatus(RefundStatus.SUCCESS);
            refundRecordService.save(refund);
        });
    }

    private void handlePayNotify(Map<String, String> params) {
        // 1.解析关键字段
        String appId = params.get("app_id");
        String orderNo = params.get("out_trade_no"); // 商户订单号
        String tradeNo = params.get("trade_no"); // 支付宝交易流水号
        String tradeStatus = params.get("trade_status");// 交易状态
        BigDecimal payAmount = new BigDecimal(params.get("total_amount"));
        // 2.校验应用ID
        if (!alipayProperties.getAppId().equals(appId)) {
            log.warn("回调app_id不匹配，期望={}, 实际={}", alipayProperties.getAppId(), appId);
            return;
        }
        // 3.只处理支付成功
        if (!"TRADE_SUCCESS".equals(tradeStatus)) {
            return;
        }
        // 4.查订单
        ParkingOrder order = parkingOrderService.lambdaQuery()
                .eq(ParkingOrder::getOrderNo, orderNo)
                .one();
        if (order == null) {
            throw new BizIllegalException("回调订单不存在：" + orderNo);
        }
        // 5.核对金额（防止篡改，通知金额必须与订单一致）
        if (order.getAmount().compareTo(payAmount) != 0) {
            throw new BizIllegalException("支付金额与订单不符，订单号：" + orderNo);
        }
        // 6.更新订单状态
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
        // 7.写支付流水
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

    /**
     * 生成退款单号：RF + 时间戳 + 6位随机数
     */
    private String generateRefundNo() {
        return "RF" + LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
    }
}
