package com.changan.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatus {
    UNPAID(0, "待支付"),
    PAID(1, "已支付"),
    CLOSED(2, "已关闭"),
    REFUNDED(3, "已退款");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    public static OrderStatus of(Integer value) {
        if (value == null) {
            return null;
        }
        for (OrderStatus status : OrderStatus.values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        return null;
    }

    public static String desc(Integer value) {
        OrderStatus status = of(value);
        return status.getDesc();
    }
}
