package com.changan.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CustomerCouponStatus implements BaseEnum {
    UNUSED(0, "未使用"),
    LOCKED(1, "锁定中"),
    USED(2, "已使用"),
    EXPIRED(3, "已过期");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    public static CustomerCouponStatus of(Integer value) {
        if (value == null) {
            return null;
        }
        for (CustomerCouponStatus status : values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        return null;
    }

    public static String desc(Integer value) {
        CustomerCouponStatus status = of(value);
        return status.getDesc();
    }
}
