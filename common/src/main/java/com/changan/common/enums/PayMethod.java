package com.changan.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PayMethod implements BaseEnum {
    ALIPAY(1, "支付宝"),
    WECHAT(2, "微信");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    public static PayMethod of(Integer value) {
        if (value == null) {
            return null;
        }
        for (PayMethod method : PayMethod.values()) {
            if (method.getValue() == value) {
                return method;
            }
        }
        return null;
    }

    public static String desc(Integer value) {
        PayMethod method = of(value);
        return method.getDesc();
    }
}
