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
import com.changan.common.constants.Constants;
import com.changan.common.enums.CustomerCouponStatus;
import com.changan.common.enums.OrderStatus;
import com.changan.common.enums.PayMethod;
import com.changan.common.enums.RefundStatus;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.model.dto.RefundFormDTO;
import com.changan.model.po.*;
import com.changan.park.service.*;
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

import cn.hutool.core.util.StrUtil;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;

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

    private final ICouponTemplateService couponTemplateService;

    private final ICustomerCouponService customerCouponService;

    @Override
    @Lock(name = Constants.Pay.CREATE_PAY_LOCK)
    public String createMyPay(Long orderId, Long couponId, Long customerId) {
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
        // 3.处理优惠券
        BigDecimal discount = processCouponForPay(order, couponId);
        // 4.计算实付（订单金额 - 优惠）= 实付
        BigDecimal paidAmount = order.getAmount().subtract(discount);
        // 5.发起H5支付
        AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
        // 5.1.设置异步通知
        request.setNotifyUrl(alipayProperties.getNotifyUrl());
        // 5.2.设置同步回跳
        request.setReturnUrl(alipayProperties.getReturnUrl());
        // 5.3.设置业务参数
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", order.getOrderNo());
        bizContent.put("total_amount", paidAmount);
        bizContent.put("subject", "停车费-" + order.getPlateNumber());
        bizContent.put("product_code", "QUICK_WAP_WAY");
        request.setBizContent(bizContent.toJSONString());
        // 6.调用支付宝，返回支付表单HTML
        try {
            return alipayClient.pageExecute(request).getBody();
        } catch (AlipayApiException e) {
            throw new BizIllegalException("创建支付失败：" + e.getMessage());
        }
    }

    /**
     * 支付前处理优惠券：返回本单生效的优惠金额
     * 三种分支：
     * couponId=null  → 不用券：解锁历史锁定的券，discount清零
     * couponId=当前已锁的券 → 重复选择同一张券：直接返回现有discount（幂等）
     * couponId=新券 → 换券/首次选券：解锁旧券，锁定新券，discount=券面额
     */
    private BigDecimal processCouponForPay(ParkingOrder order, Long couponId) {
        return transactionTemplate.execute(status -> {
            // 1.查当前订单锁定的券
            CustomerCoupon lockedCoupon = null;
            if (order.getDiscount() != null && order.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
                lockedCoupon = customerCouponService.lambdaQuery()
                        .eq(CustomerCoupon::getOrderId, order.getId())
                        .eq(CustomerCoupon::getStatus, CustomerCouponStatus.LOCKED)
                        .one();
            }
            // 2.不用券，解锁旧券，discount清0
            if (couponId == null) {
                if (lockedCoupon != null) {
                    // 解锁券
                    unlockCoupon(lockedCoupon.getId());
                    // discount清0
                    updateDiscount(order.getId(), BigDecimal.ZERO);
                }
                return BigDecimal.ZERO;
            }
            // 3.选了券，查券校验
            CustomerCoupon coupon = customerCouponService.getById(couponId);
            if (coupon == null || !coupon.getCustomerId().equals(order.getCustomerId())) {
                throw new BizIllegalException("优惠券不存在");
            }
            if (coupon.getStatus() != CustomerCouponStatus.UNUSED) {
                // 重复选同一张已锁定的券，直接复用
                if (lockedCoupon != null && lockedCoupon.getId().equals(couponId)
                        && coupon.getStatus() == CustomerCouponStatus.LOCKED) {
                    return order.getDiscount();
                }
                throw new BizIllegalException("该优惠券不可用");
            }
            // 4.查模板，校验门槛与有效期
            if (coupon.getExpireTime().isBefore(LocalDateTime.now())) {
                throw new BizIllegalException("该券已过期");
            }
            CouponTemplate template = couponTemplateService.getById(coupon.getTemplateId());
            if (template == null) {
                throw new BizIllegalException("该优惠券不可用");
            }
            if (order.getAmount().compareTo(template.getThreshold()) < 0) {
                throw new BizIllegalException("订单金额未达优惠券使用门槛");
            }
            // 5.换券
            if (lockedCoupon != null && !lockedCoupon.getId().equals(couponId)) {
                // 先解锁旧券
                unlockCoupon(lockedCoupon.getId());
            }
            // 6.锁定新券
            boolean locked = customerCouponService.lambdaUpdate()
                    .eq(CustomerCoupon::getId, couponId)
                    .eq(CustomerCoupon::getStatus, CustomerCouponStatus.UNUSED)
                    .apply("expire_time > now()")
                    .set(CustomerCoupon::getStatus, CustomerCouponStatus.LOCKED)
                    .set(CustomerCoupon::getOrderId, order.getId())
                    .update();
            if (!locked) {
                throw new BizIllegalException("优惠券已被使用或已过期");
            }
            // 7.更新订单discount字段
            updateDiscount(order.getId(), template.getAmount());
            return template.getAmount();
        });
    }

    /**
     * 解锁优惠券
     */
    private void unlockCoupon(Long couponId) {
        customerCouponService.lambdaUpdate()
                .eq(CustomerCoupon::getId, couponId)
                .eq(CustomerCoupon::getStatus, CustomerCouponStatus.LOCKED)
                .set(CustomerCoupon::getStatus, CustomerCouponStatus.UNUSED)
                .set(CustomerCoupon::getOrderId, null)
                .update();
    }

    /**
     * 更新订单优惠金额
     */
    private void updateDiscount(Long orderId, BigDecimal discount) {
        parkingOrderService.lambdaUpdate()
                .eq(ParkingOrder::getId, orderId)
                .eq(ParkingOrder::getStatus, OrderStatus.UNPAID)
                .set(ParkingOrder::getDiscount, discount)
                .update();
    }

    @Override
    @Lock(name = Constants.Pay.CREATE_PAY_LOCK)
    public void cancelOrder(Long orderId) {
        ParkingOrder order = parkingOrderService.getById(orderId);
        if (order == null) {
            throw new BizIllegalException("订单不存在");
        }
        // 重复点击视为成功，保证接口幂等
        if (order.getStatus() == OrderStatus.CLOSED) {
            return;
        }
        if (order.getStatus() != OrderStatus.UNPAID) {
            throw new BizIllegalException("只有待支付订单才能关闭");
        }
        // 必须先关闭支付宝交易，再关闭本地订单
        closeAlipayTrade(order.getOrderNo());
        // 本地关闭仍然使用原来的 CAS 和优惠券释放逻辑
        parkingOrderService.closeOrder(orderId);
    }

    /**
     * 关闭支付宝交易
     * 已经不存在支付宝交易时，也允许关闭本地订单
     */
    private void closeAlipayTrade(String orderNo) {
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();

        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderNo);
        request.setBizContent(bizContent.toJSONString());

        AlipayTradeCloseResponse response;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            log.error("调用支付宝关单异常，orderNo={}", orderNo, e);
            throw new BizIllegalException("关闭支付宝交易失败，订单未关闭，请稍后重试");
        }

        if (response.isSuccess()) {
            log.info("支付宝关单成功，orderNo={}, tradeNo={}", orderNo, response.getTradeNo());
            return;
        }

        // 用户从未拉起支付宝，或支付宝侧不存在该交易，可以直接关闭本地订单
        if ("ACQ.TRADE_NOT_EXIST".equals(response.getSubCode())) {
            log.info("支付宝交易不存在，直接关闭本地订单，orderNo={}", orderNo);
            return;
        }

        log.error("支付宝关单失败，orderNo={}, code={}, subCode={}, subMsg={}",
                orderNo,
                response.getCode(),
                response.getSubCode(),
                response.getSubMsg());
        throw new BizIllegalException("关闭支付宝交易失败：" + response.getSubMsg());
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
    @Lock(name = Constants.Pay.REFUND_LOCK)
    public void refund(RefundFormDTO dto) {
        // 1.查订单
        ParkingOrder order = parkingOrderService.lambdaQuery()
                .eq(ParkingOrder::getOrderNo, dto.getOrderNo())
                .one();
        if (order == null) {
            throw new BizIllegalException("订单不存在");
        }
        // 2.查询是否已经有退款记录
        RefundRecord refund = refundRecordService.lambdaQuery()
                .eq(RefundRecord::getOrderId, order.getId())
                .orderByDesc(RefundRecord::getCreateTime)
                .last("LIMIT 1")
                .one();
        // 已退款订单重复调用，直接视为成功
        if (order.getStatus() == OrderStatus.REFUNDED) {
            if (refund != null && refund.getStatus() == RefundStatus.SUCCESS) {
                return;
            }
            throw new BizIllegalException("订单已退款，但退款记录异常，请人工核查");
        }
        // 只允许已支付订单退款
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BizIllegalException("只有已支付的订单才能退款");
        }
        // 3.查支付流水
        PaymentRecord payment = paymentRecordService.lambdaQuery()
                .eq(PaymentRecord::getOrderId, order.getId())
                .orderByDesc(PaymentRecord::getPayTime)
                .last("LIMIT 1")
                .one();
        if (payment == null) {
            throw new BizIllegalException("支付流水不存在，无法退款");
        }
        if (payment.getAmount().compareTo(order.getPaid()) != 0) {
            throw new BizIllegalException("支付金额与订单实付金额不一致，请人工核查");
        }
        // 4.没有退款记录时，先落库为处理中
        if (refund == null) {
            refund = new RefundRecord();
            refund.setOrderId(order.getId());
            refund.setPaymentId(payment.getId());
            refund.setRefundNo(generateRefundNo());
            refund.setChannelRefundNo(null);
            refund.setAmount(payment.getAmount());
            refund.setReason(dto.getReason());
            refund.setStatus(RefundStatus.PROCESS);

            // 这一步必须成功，否则不能调用支付宝
            refundRecordService.save(refund);
        } else {
            // 同一个订单已经退成功，直接返回
            if (refund.getStatus() == RefundStatus.SUCCESS) {
                return;
            }

            if (refund.getAmount().compareTo(payment.getAmount()) != 0
                    || !refund.getPaymentId().equals(payment.getId())) {
                throw new BizIllegalException("已有退款记录与支付信息不一致，请人工核查");
            }

            // 上一次明确失败后重新点击，继续复用原退款单号
            if (refund.getStatus() == RefundStatus.FAIL) {
                boolean retryUpdated = refundRecordService.lambdaUpdate()
                        .eq(RefundRecord::getId, refund.getId())
                        .eq(RefundRecord::getStatus, RefundStatus.FAIL)
                        .set(RefundRecord::getStatus, RefundStatus.PROCESS)
                        .update();

                if (!retryUpdated) {
                    throw new BizIllegalException("退款状态已变化，请刷新后重试");
                }

                refund.setStatus(RefundStatus.PROCESS);
            }

            if (refund.getStatus() != RefundStatus.PROCESS) {
                throw new BizIllegalException("退款状态异常，请人工核查");
            }
        }

        Long refundId = refund.getId();

        // 重试必须复用原来落库的退款原因和退款单号
        String refundNo = refund.getRefundNo();
        String refundReason = refund.getReason();

        // 5.调用支付宝退款
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", order.getOrderNo());
        bizContent.put("refund_amount", refund.getAmount());
        bizContent.put("out_request_no", refundNo);         // 幂等键
        bizContent.put("refund_reason", refundReason);   // 退款原因
        request.setBizContent(bizContent.toJSONString());

        AlipayTradeRefundResponse response;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            // 网络超时不能判定失败，退款记录继续保持 PROCESS
            log.error("调用支付宝退款结果未知，refundNo={}", refundNo, e);
            throw new BizIllegalException("退款请求结果未知，退款记录仍为处理中，请稍后重试");
        }

        // 支付宝明确返回失败，退款记录改为失败，订单仍保持已支付
        if (!response.isSuccess()) {
            refundRecordService.lambdaUpdate()
                    .eq(RefundRecord::getId, refundId)
                    .eq(RefundRecord::getStatus, RefundStatus.PROCESS)
                    .set(RefundRecord::getStatus, RefundStatus.FAIL)
                    .update();

            log.error("支付宝退款失败，refundNo={}, code={}, subCode={}, subMsg={}",
                    refundNo,
                    response.getCode(),
                    response.getSubCode(),
                    response.getSubMsg());

            throw new BizIllegalException("退款失败：" + response.getSubMsg());
        }

        // 6. 支付宝退款成功，更新退款记录和订单状态
        String channelRefundNo = StrUtil.isBlank(response.getRefundSettlementId())
                ? null
                : response.getRefundSettlementId();

        LocalDateTime refundTime = response.getGmtRefundPay() == null
                ? LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
                : LocalDateTime.ofInstant(
                response.getGmtRefundPay().toInstant(),
                ZoneId.of("Asia/Shanghai"));

        transactionTemplate.executeWithoutResult(status -> {
            boolean refundUpdated = refundRecordService.lambdaUpdate()
                    .eq(RefundRecord::getId, refundId)
                    .eq(RefundRecord::getStatus, RefundStatus.PROCESS)
                    .set(RefundRecord::getStatus, RefundStatus.SUCCESS)
                    .set(RefundRecord::getChannelRefundNo, channelRefundNo)
                    .set(RefundRecord::getRefundTime, refundTime)
                    .update();

            if (!refundUpdated) {
                throw new BizIllegalException("退款记录状态已变化，请刷新后重试");
            }

            boolean orderUpdated = parkingOrderService.lambdaUpdate()
                    .eq(ParkingOrder::getId, order.getId())
                    .eq(ParkingOrder::getStatus, OrderStatus.PAID)
                    .set(ParkingOrder::getStatus, OrderStatus.REFUNDED)
                    .update();

            if (!orderUpdated) {
                ParkingOrder latest = parkingOrderService.getById(order.getId());
                if (latest == null || latest.getStatus() != OrderStatus.REFUNDED) {
                    throw new BizIllegalException("退款成功但订单状态更新失败，请人工核查");
                }
            }
        });

        log.info("退款成功，orderNo={}, refundNo={}", order.getOrderNo(), refundNo);
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
        // 5.核对金额（防止篡改，通知金额必须等于实付=amount-discount）
        BigDecimal expected = order.getAmount().subtract(
                order.getDiscount() == null ? BigDecimal.ZERO : order.getDiscount());
        if (expected.compareTo(payAmount) != 0) {
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
        // 7.核销优惠券
        boolean couponUsed = customerCouponService.lambdaUpdate()
                .eq(CustomerCoupon::getOrderId, order.getId())
                .eq(CustomerCoupon::getStatus, CustomerCouponStatus.LOCKED)
                .set(CustomerCoupon::getStatus, CustomerCouponStatus.USED)
                .update();
        if (!couponUsed) {
            log.error("支付成功但优惠券核销失败，orderId={}, 需人工核查", order.getId());
        }
        // 8.写支付流水
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
