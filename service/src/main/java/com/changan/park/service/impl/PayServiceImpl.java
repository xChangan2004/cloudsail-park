package com.changan.park.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.changan.common.config.alipay.AlipayProperties;
import com.changan.common.config.redisson.annotations.Lock;
import com.changan.common.constants.Constants;
import com.changan.common.enums.*;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.model.dto.RefundFormDTO;
import com.changan.model.po.*;
import com.changan.park.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import cn.hutool.core.util.StrUtil;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;

import static com.changan.common.constants.Constants.Order.PAY_TIMEOUT;
import static com.changan.common.constants.Constants.Order.TIME_FORMAT;

/**
 * 支付域核心服务。
 * <p>
 * 一个订单可以有多笔支付尝试，每次发起支付使用新的 paymentNo。
 * 优惠券按订单维度锁定，支付成功后才核销，取消或超时关闭时释放。
 */
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

    /**
     * 发起支付宝 APP 支付。
     * <p>
     * 每次调用都会关闭旧 PENDING 流水，重新处理优惠券，创建新的 PENDING 流水，
     * 并将新的 paymentNo 作为支付宝 out_trade_no。
     */
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

        closePendingPaymentAttempts(orderId);

        PaymentAttempt attempt;
        try {
            attempt = transactionTemplate.execute(status ->
                    preparePaymentAttempt(orderId, couponId));
        } catch (RuntimeException e) {
            // 旧支付已关闭，新支付准备失败时不要留下无支付页面的锁券状态。
            releaseCouponIfNoPendingPayment(orderId);
            throw e;
        }
        if (attempt == null) {
            throw new BizIllegalException("创建支付流水失败");
        }

        PaymentRecord payment = attempt.payment();

        // 5.发起APP支付
        AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
        // 5.1.设置异步通知
        request.setNotifyUrl(alipayProperties.getNotifyUrl());
        // 5.2.设置业务参数
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", payment.getPaymentNo());
        bizContent.put("total_amount", payment.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        bizContent.put("subject", "停车费-" + order.getPlateNumber());
        bizContent.put("product_code", "QUICK_MSECURITY_PAY");
        bizContent.put("timeout_express", "30m");
        bizContent.put("passback_params", buildPassbackParams(order.getOrderNo(), attempt.couponId()));
        request.setBizContent(bizContent.toJSONString());
        // 6.调用支付宝，返回支付信息
        AlipayTradeAppPayResponse response;
        try {
            response = alipayClient.sdkExecute(request);
            if (StrUtil.isBlank(response.getBody())) {
                markPaymentFailed(payment.getId());
                throw new BizIllegalException("创建支付失败，请稍后重试");
            }
            return response.getBody();
        } catch (AlipayApiException e) {
            log.error("创建支付宝APP支付失败，paymentNo={}", payment.getPaymentNo(), e);
            markPaymentFailed(payment.getId());
            throw new BizIllegalException("创建支付失败，请稍后重试");
        }
    }

    /**
     * 在订单行锁内准备本次支付快照。
     * <p>
     * couponId 为空：释放旧券并清空优惠。
     * couponId 为当前锁定券：复用现有 discount。
     * couponId 为新券：解锁旧券，锁定新券，并覆盖 order.discount。
     */
    private PaymentAttempt preparePaymentAttempt(Long orderId, Long couponId) {
        ParkingOrder order = getOrderForUpdate(orderId);
        if (order == null || order.getStatus() != OrderStatus.UNPAID) {
            throw new BizIllegalException("当前订单状态不支持支付");
        }

        CustomerCoupon lockedCoupon = findLockedCoupon(orderId);
        BigDecimal discount = BigDecimal.ZERO;

        if (couponId == null) {
            unlockAllCoupons(orderId);
        } else {
            CustomerCoupon coupon = customerCouponService.getById(couponId);
            if (coupon == null || !Objects.equals(coupon.getCustomerId(), order.getCustomerId())) {
                throw new BizIllegalException("优惠券不存在");
            }

            boolean sameLockedCoupon = coupon.getStatus() == CustomerCouponStatus.LOCKED
                    && Objects.equals(coupon.getOrderId(), orderId)
                    && lockedCoupon != null
                    && Objects.equals(lockedCoupon.getId(), couponId);

            if (!sameLockedCoupon && coupon.getStatus() != CustomerCouponStatus.UNUSED) {
                throw new BizIllegalException("该优惠券不可用");
            }
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

            if (sameLockedCoupon) {
                discount = order.getDiscount();
                if (discount == null || discount.compareTo(template.getAmount()) != 0) {
                    throw new BizIllegalException("当前锁定券优惠金额异常，请人工核查");
                }
            } else {
                if (lockedCoupon != null) {
                    unlockCoupon(lockedCoupon.getId(), orderId);
                }
                lockCoupon(coupon, orderId);
                discount = template.getAmount();
            }
        }

        BigDecimal payAmount = order.getAmount().subtract(discount);
        if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizIllegalException("优惠后实付金额必须大于0元");
        }

        updateOrderDiscountOrThrow(orderId, discount);

        PaymentRecord payment = new PaymentRecord();
        payment.setOrderId(orderId);
        payment.setPaymentNo(generatePaymentNo());
        payment.setPayMethod(PayMethod.ALIPAY);
        payment.setAmount(payAmount);
        payment.setStatus(PaymentStatus.PENDING);
        paymentRecordService.save(payment);

        return new PaymentAttempt(payment, couponId);
    }

    /**
     * 关闭该订单此前所有待支付尝试。
     * 必须先关闭支付宝旧交易，再创建新的支付页面，防止旧页面继续按旧金额支付。
     */
    private void closePendingPaymentAttempts(Long orderId) {
        List<PaymentRecord> pendingPayments = paymentRecordService.lambdaQuery()
                .eq(PaymentRecord::getOrderId, orderId)
                .eq(PaymentRecord::getStatus, PaymentStatus.PENDING)
                .orderByAsc(PaymentRecord::getCreateTime)
                .list();

        for (PaymentRecord payment : pendingPayments) {
            closeAlipayTrade(payment.getPaymentNo());
            finishPaymentAttempt(payment.getId(), PaymentStatus.CLOSED, false);
        }
    }

    /**
     * 支付成功后的兜底清理：关闭该订单其他仍未支付的支付宝交易。
     */
    private void closeRemainingPendingPayments(Long orderId) {
        List<PaymentRecord> pendingPayments = paymentRecordService.lambdaQuery()
                .eq(PaymentRecord::getOrderId, orderId)
                .eq(PaymentRecord::getStatus, PaymentStatus.PENDING)
                .list();

        for (PaymentRecord payment : pendingPayments) {
            try {
                closeAlipayTrade(payment.getPaymentNo());
                finishPaymentAttempt(payment.getId(), PaymentStatus.CLOSED, false);
            } catch (Exception e) {
                log.error("支付成功后关闭剩余待支付流水失败，paymentNo={}", payment.getPaymentNo(), e);
            }
        }
    }

    private void markPaymentFailed(Long paymentId) {
        finishPaymentAttempt(paymentId, PaymentStatus.FAILED, true);
    }

    private void finishPaymentAttempt(Long paymentId, PaymentStatus targetStatus,
                                      boolean releaseCouponWhenNoPending) {
        transactionTemplate.executeWithoutResult(status ->
                finishPaymentAttemptInternal(paymentId, targetStatus, releaseCouponWhenNoPending));
    }

    private void finishPaymentAttemptInternal(Long paymentId, PaymentStatus targetStatus,
                                              boolean releaseCouponWhenNoPending) {
        PaymentRecord payment = paymentRecordService.getById(paymentId);
        if (payment == null || payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }

        boolean updated = paymentRecordService.lambdaUpdate()
                .eq(PaymentRecord::getId, paymentId)
                .eq(PaymentRecord::getStatus, PaymentStatus.PENDING)
                .set(PaymentRecord::getStatus, targetStatus)
                .update();

        if (!updated) {
            throw new BizIllegalException("支付状态已变化，请刷新后重试");
        }

        if (releaseCouponWhenNoPending) {
            releaseCouponIfNoPendingPaymentInternal(payment.getOrderId());
        }
    }

    /**
     * 如果没有其他待支付流水，释放当前订单锁定的优惠券并清空 discount。
     */
    private void releaseCouponIfNoPendingPayment(Long orderId) {
        transactionTemplate.executeWithoutResult(status ->
                releaseCouponIfNoPendingPaymentInternal(orderId));
    }

    private void releaseCouponIfNoPendingPaymentInternal(Long orderId) {
        ParkingOrder order = getOrderForUpdate(orderId);
        if (order == null || order.getStatus() != OrderStatus.UNPAID) {
            return;
        }

        Long pendingCount = paymentRecordService.lambdaQuery()
                .eq(PaymentRecord::getOrderId, orderId)
                .eq(PaymentRecord::getStatus, PaymentStatus.PENDING)
                .count();

        if (pendingCount == 0) {
            unlockAllCoupons(orderId);
            updateOrderDiscountOrThrow(orderId, BigDecimal.ZERO);
        }
    }


    private ParkingOrder getOrderForUpdate(Long orderId) {
        return parkingOrderService.lambdaQuery()
                .eq(ParkingOrder::getId, orderId)
                .last("FOR UPDATE")
                .one();
    }

    private void updateOrderDiscountOrThrow(Long orderId, BigDecimal discount) {
        boolean updated = parkingOrderService.lambdaUpdate()
                .eq(ParkingOrder::getId, orderId)
                .eq(ParkingOrder::getStatus, OrderStatus.UNPAID)
                .set(ParkingOrder::getDiscount, discount)
                .update();

        if (!updated) {
            throw new BizIllegalException("订单状态已变化，请刷新后重试");
        }
    }

    private CustomerCoupon findLockedCoupon(Long orderId) {
        return customerCouponService.lambdaQuery()
                .eq(CustomerCoupon::getOrderId, orderId)
                .eq(CustomerCoupon::getStatus, CustomerCouponStatus.LOCKED)
                .last("LIMIT 1")
                .one();
    }

    private void lockCoupon(CustomerCoupon coupon, Long orderId) {
        boolean locked = customerCouponService.lambdaUpdate()
                .eq(CustomerCoupon::getId, coupon.getId())
                .eq(CustomerCoupon::getCustomerId, coupon.getCustomerId())
                .eq(CustomerCoupon::getStatus, CustomerCouponStatus.UNUSED)
                .apply("expire_time > now()")
                .set(CustomerCoupon::getStatus, CustomerCouponStatus.LOCKED)
                .set(CustomerCoupon::getOrderId, orderId)
                .update();

        if (!locked) {
            throw new BizIllegalException("优惠券已被使用或已过期");
        }
    }

    /**
     * 解锁优惠券
     */
    private void unlockCoupon(Long couponId, Long orderId) {
        boolean unlocked = customerCouponService.lambdaUpdate()
                .eq(CustomerCoupon::getId, couponId)
                .eq(CustomerCoupon::getOrderId, orderId)
                .eq(CustomerCoupon::getStatus, CustomerCouponStatus.LOCKED)
                .set(CustomerCoupon::getStatus, CustomerCouponStatus.UNUSED)
                .set(CustomerCoupon::getOrderId, null)
                .update();

        if (!unlocked) {
            throw new BizIllegalException("优惠券状态已变化，请刷新后重试");
        }
    }

    private void unlockAllCoupons(Long orderId) {
        customerCouponService.lambdaUpdate()
                .eq(CustomerCoupon::getOrderId, orderId)
                .eq(CustomerCoupon::getStatus, CustomerCouponStatus.LOCKED)
                .set(CustomerCoupon::getStatus, CustomerCouponStatus.UNUSED)
                .set(CustomerCoupon::getOrderId, null)
                .update();
    }


    /**
     * 后台关闭订单：先关闭支付宝待支付交易，再关闭本地订单并释放优惠券。
     */
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
        closePendingPaymentAttempts(orderId);
        // 本地关闭仍然使用原来的 CAS 和优惠券释放逻辑
        parkingOrderService.closeOrder(orderId);
    }

    /**
     * 调用支付宝关闭指定支付尝试。
     * ACQ.TRADE_NOT_EXIST 表示用户从未拉起支付宝，可以继续关闭本地流水。
     */
    private void closeAlipayTrade(String paymentNo) {
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();

        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", paymentNo);
        request.setBizContent(bizContent.toJSONString());

        AlipayTradeCloseResponse response;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            log.error("调用支付宝关单异常，paymentNo={}", paymentNo, e);
            throw new BizIllegalException("关闭支付宝交易失败，订单未关闭，请稍后重试");
        }

        if (response.isSuccess()) {
            log.info("支付宝关单成功，paymentNo={}, tradeNo={}", paymentNo, response.getTradeNo());
            return;
        }

        // 用户从未拉起支付宝，或支付宝侧不存在该交易，可以直接关闭本地订单
        if ("ACQ.TRADE_NOT_EXIST".equals(response.getSubCode())) {
            log.info("支付宝交易不存在，继续处理本地流水，paymentNo={}", paymentNo);
            return;
        }

        log.error("支付宝关单失败，paymentNo={}, code={}, subCode={}, subMsg={}",
                paymentNo,
                response.getCode(),
                response.getSubCode(),
                response.getSubMsg());
        throw new BizIllegalException("关闭支付宝交易失败：" + response.getSubMsg());
    }

    /**
     * 支付宝异步通知入口：先验签，再在事务中完成流水、订单、优惠券的联动更新。
     */
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
        Long paidOrderId;
        try {
            paidOrderId = transactionTemplate.execute(status -> handlePayNotify(params));
        } catch (Exception e) {
            log.error("支付宝回调业务处理异常，params={}", params, e);
            return false;
        }

        if (paidOrderId != null) {
            closeRemainingPendingPayments(paidOrderId);
        }

        return true;

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
                .eq(PaymentRecord::getStatus, PaymentStatus.PAID)
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
        // 优先用支付宝交易号：部分环境（含沙箱）按 out_trade_no 检索不到交易
        if (StrUtil.isNotBlank(payment.getTransactionId())) {
            bizContent.put("trade_no", payment.getTransactionId());
        } else {
            bizContent.put("out_trade_no", payment.getPaymentNo());
        }
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
        // 支付宝没有独立的退款单号，refund_settlement_id 只在银行间联场景返回；
        // 退款不会产生新交易，响应里的 trade_no 就是原支付交易号，这里落库便于和支付流水对上
        String channelRefundNo = StrUtil.isNotBlank(response.getTradeNo())
                ? response.getTradeNo()
                : payment.getTransactionId();

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

    /**
     * 处理支付成功通知。
     * <p>
     * 根据 out_trade_no(paymentNo) 定位 PaymentRecord，再通过 passback_params
     * 校验业务订单和 couponId，最终完成 PENDING -> PAID、订单 PAID、券 USED。
     */
    private Long handlePayNotify(Map<String, String> params) {
        String appId = params.get("app_id");
        String paymentNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");
        String totalAmount = params.get("total_amount");

        if (StrUtil.isBlank(paymentNo) || StrUtil.isBlank(tradeStatus) || StrUtil.isBlank(totalAmount)) {
            throw new BizIllegalException("支付宝回调缺少必要参数");
        }
        if (!Objects.equals(alipayProperties.getAppId(), appId)) {
            throw new BizIllegalException("回调app_id不匹配");
        }

        PayCallbackContext callbackContext = parsePassbackParams(params.get("passback_params"));
        PaymentRecord payment = paymentRecordService.lambdaQuery()
                .eq(PaymentRecord::getPaymentNo, paymentNo)
                .one();
        if (payment == null) {
            throw new BizIllegalException("回调支付流水不存在：" + paymentNo);
        }

        ParkingOrder order = getOrderForUpdate(payment.getOrderId());
        if (order == null || !Objects.equals(order.getOrderNo(), callbackContext.orderNo())) {
            throw new BizIllegalException("回调订单与支付流水不匹配");
        }

        if ("WAIT_BUYER_PAY".equals(tradeStatus)) {
            return null;
        }
        if ("TRADE_CLOSED".equals(tradeStatus)) {
            finishPaymentAttemptInternal(payment.getId(), PaymentStatus.CLOSED, true);
            return null;
        }
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            return null;
        }

        BigDecimal payAmount;
        try {
            payAmount = new BigDecimal(totalAmount);
        } catch (NumberFormatException e) {
            throw new BizIllegalException("回调支付金额格式错误");
        }
        if (payment.getAmount().compareTo(payAmount) != 0) {
            throw new BizIllegalException("支付金额与支付流水不一致：" + paymentNo);
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            if (Objects.equals(payment.getTransactionId(), tradeNo)) {
                return order.getId();
            }
            throw new BizIllegalException("支付流水已完成但交易号不一致：" + paymentNo);
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BizIllegalException("非待支付流水收到成功通知：" + paymentNo);
        }
        if (StrUtil.isBlank(tradeNo)) {
            throw new BizIllegalException("支付宝回调缺少交易号");
        }

        CustomerCoupon coupon = validateCallbackCoupon(callbackContext.couponId(), order, payAmount);
        LocalDateTime notifyTime = LocalDateTime.now();

        boolean paymentUpdated = paymentRecordService.lambdaUpdate()
                .eq(PaymentRecord::getId, payment.getId())
                .eq(PaymentRecord::getStatus, PaymentStatus.PENDING)
                .set(PaymentRecord::getStatus, PaymentStatus.PAID)
                .set(PaymentRecord::getTransactionId, tradeNo)
                .set(PaymentRecord::getPayTime, notifyTime)
                .set(PaymentRecord::getCallbackTime, notifyTime)
                .set(PaymentRecord::getCallbackContent, JSONObject.toJSONString(params))
                .update();
        if (!paymentUpdated) {
            throw new BizIllegalException("支付流水状态已变化，请稍后重试");
        }

        boolean orderUpdated = parkingOrderService.lambdaUpdate()
                .eq(ParkingOrder::getId, order.getId())
                .eq(ParkingOrder::getStatus, OrderStatus.UNPAID)
                .set(ParkingOrder::getStatus, OrderStatus.PAID)
                .set(ParkingOrder::getPaid, payAmount)
                .set(ParkingOrder::getPaidTime, notifyTime)
                .update();
        if (!orderUpdated) {
            throw new BizIllegalException("订单状态已变化，支付流水保持待处理，请人工核查：" + paymentNo);
        }

        if (coupon != null) {
            boolean couponUsed = customerCouponService.lambdaUpdate()
                    .eq(CustomerCoupon::getId, coupon.getId())
                    .eq(CustomerCoupon::getCustomerId, order.getCustomerId())
                    .eq(CustomerCoupon::getStatus, CustomerCouponStatus.LOCKED)
                    .eq(CustomerCoupon::getOrderId, order.getId())
                    .set(CustomerCoupon::getStatus, CustomerCouponStatus.USED)
                    .update();
            if (!couponUsed) {
                throw new BizIllegalException("支付成功但优惠券核销失败，请人工核查");
            }
        }

        log.info("支付入账成功，orderNo={}, paymentNo={}, tradeNo={}",
                order.getOrderNo(), paymentNo, tradeNo);
        return order.getId();
    }

    /**
     * 校验本次回调对应的优惠券，防止调用方伪造成其他优惠或重复核销。
     */
    private CustomerCoupon validateCallbackCoupon(Long couponId, ParkingOrder order, BigDecimal payAmount) {
        if (couponId == null) {
            Long lockedCount = customerCouponService.lambdaQuery()
                    .eq(CustomerCoupon::getOrderId, order.getId())
                    .eq(CustomerCoupon::getStatus, CustomerCouponStatus.LOCKED)
                    .count();
            if (lockedCount > 0) {
                throw new BizIllegalException("无券支付但订单存在锁定优惠券，请人工核查");
            }
            BigDecimal discount = order.getDiscount() == null ? BigDecimal.ZERO : order.getDiscount();
            if (discount.compareTo(BigDecimal.ZERO) != 0) {
                throw new BizIllegalException("无券支付但订单存在优惠金额，请人工核查");
            }
            return null;
        }

        CustomerCoupon coupon = customerCouponService.getById(couponId);
        if (coupon == null || !Objects.equals(coupon.getCustomerId(), order.getCustomerId())) {
            throw new BizIllegalException("回调优惠券不存在");
        }
        if (coupon.getStatus() != CustomerCouponStatus.LOCKED
                || !Objects.equals(coupon.getOrderId(), order.getId())) {
            throw new BizIllegalException("回调优惠券状态不可用");
        }

        CouponTemplate template = couponTemplateService.getById(coupon.getTemplateId());
        if (template == null) {
            throw new BizIllegalException("回调优惠券模板不存在");
        }

        BigDecimal currentDiscount = order.getDiscount() == null ? BigDecimal.ZERO : order.getDiscount();
        if (currentDiscount.compareTo(template.getAmount()) != 0) {
            throw new BizIllegalException("订单优惠金额与回调优惠券不一致，请人工核查");
        }

        BigDecimal expectedAmount = order.getAmount().subtract(template.getAmount());
        if (expectedAmount.compareTo(payAmount) != 0) {
            throw new BizIllegalException("优惠后支付金额不一致，请人工核查");
        }
        return coupon;
    }

    /**
     * 生成退款单号：RF + 时间戳 + 6位随机数
     */
    private String generateRefundNo() {
        return "RF" + LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
                .format(TIME_FORMAT)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
    }

    /**
     * 解析 passback_params
     */
    private PayCallbackContext parsePassbackParams(String passbackParams) {
        if (StrUtil.isBlank(passbackParams)) {
            throw new BizIllegalException("支付宝回调缺少透传参数");
        }
        String raw;
        try {
            raw = URLDecoder.decode(passbackParams, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            log.error("支付宝透传参数解码失败，passbackParams:{}", passbackParams, e);
            throw new BizIllegalException("透传参数解码失败");
        }

        Map<String, String> map = new HashMap<>();
        String[] pairs = raw.split("&");
        for (String pair : pairs) {
            // 跳过空片段，防御连续&符号
            if (StrUtil.isBlank(pair)) {
                continue;
            }
            // 只分割第一个=，value允许包含=
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            }
        }

        String orderNo = map.get("orderNo");
        if (StrUtil.isBlank(orderNo)) {
            throw new BizIllegalException("支付宝回调透传订单号缺失");
        }

        Long couponId = null;
        String couponIdStr = map.get("couponId");
        if (StrUtil.isNotBlank(couponIdStr)) {
            try {
                couponId = Long.parseLong(couponIdStr);
            } catch (NumberFormatException e) {
                log.error("支付宝回调couponId格式错误，couponIdStr:{}", couponIdStr, e);
                throw new BizIllegalException("支付宝回调 couponId 格式错误");
            }
        }
        return new PayCallbackContext(orderNo, couponId);
    }

    /**
     * 透传业务订单号和优惠券 ID。
     * 支付宝要求 passback_params 发送前整体 URLEncode。
     */
    private String buildPassbackParams(String orderNo, Long couponId) {
        StringBuilder sb = new StringBuilder();
        sb.append("orderNo=").append(orderNo);
        if (couponId != null) {
            sb.append("&couponId=").append(couponId);
        }
        // 再整体URLEncode
        return URLEncoder.encode(sb.toString(), StandardCharsets.UTF_8);
    }

    /**
     * 生成每次支付尝试唯一的支付宝商户单号。
     */
    private String generatePaymentNo() {
        return "CPP" + LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
                .format(TIME_FORMAT)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
    }

    private record PaymentAttempt(PaymentRecord payment, Long couponId) {
    }

    private record PayCallbackContext(String orderNo, Long couponId) {
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void closeExpiredPayments() {
        List<PaymentRecord> expiredPayments = paymentRecordService.lambdaQuery()
                .eq(PaymentRecord::getStatus, PaymentStatus.PENDING)
                .le(PaymentRecord::getCreateTime, LocalDateTime.now().minus(PAY_TIMEOUT))
                .orderByAsc(PaymentRecord::getCreateTime)
                .last("LIMIT 100")
                .list();

        for (PaymentRecord payment : expiredPayments) {
            try {
                closeAlipayTrade(payment.getPaymentNo());
                finishPaymentAttempt(payment.getId(), PaymentStatus.CLOSED, true);
            } catch (Exception e) {
                log.error("关闭超时支付失败，paymentNo={}", payment.getPaymentNo(), e);
            }
        }
    }
}
