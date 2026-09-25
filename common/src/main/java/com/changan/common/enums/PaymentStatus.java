package com.changan.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付流水状态。
 * 一笔订单可以产生多笔支付尝试，但正常情况下最多只有一笔最终变为 PAID。
 */
@Getter
@AllArgsConstructor
public enum PaymentStatus implements BaseEnum {

    /** 已创建支付宝支付页面，等待用户支付。 */
    PENDING(0, "待支付"),

    /** 已收到支付宝成功回调，支付已经入账。 */
    PAID(1, "支付成功"),

    /** 主动关闭、切换优惠重新支付或超时后关闭。 */
    CLOSED(2, "已关闭"),

    /** 调用支付宝创建支付时明确失败。 */
    FAILED(3, "支付失败");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    public static PaymentStatus of(Integer value) {
        if (value == null) {
            return null;
        }
        for (PaymentStatus status : values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        return null;
    }

    public static String desc(Integer value) {
        PaymentStatus status = of(value);
        return status == null ? null : status.getDesc();
    }
}

