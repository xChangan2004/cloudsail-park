package com.changan.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RefundStatus {
    PROCESS(0, "处理中"),
    SUCCESS(1, "成功"),
    FAIL(2, "失败");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    public static RefundStatus of(Integer value) {
        if (value == null) {
            return null;
        }
        for (RefundStatus status : RefundStatus.values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        return null;
    }

    public static String desc(Integer value) {
        RefundStatus status = of(value);
        return status.getDesc();
    }
}
