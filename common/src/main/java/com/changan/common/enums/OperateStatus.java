package com.changan.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OperateStatus implements BaseEnum {

    FAIL(0, "失败"),
    SUCCESS(1, "成功");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    public static OperateStatus of(Integer value) {
        if (value == null) {
            return null;
        }
        for (OperateStatus status : values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        return null;
    }

    public static String desc(Integer value) {
        OperateStatus status = of(value);
        return status == null ? null : status.getDesc();
    }
}
