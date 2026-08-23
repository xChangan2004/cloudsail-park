package com.changan.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RuleFeeType implements BaseEnum {
    STANDARD(1, "标准"),
    PERIOD(2, "时段"),
    ;

    @JsonValue
    @EnumValue
    private final int value;
    private final String desc;

    public static RuleFeeType of(Integer value) {
        if (value == null) {
            return null;
        }
        for (RuleFeeType ruleFeeType : values()) {
            if (ruleFeeType.getValue() == value) {
                return ruleFeeType;
            }
        }
        return null;
    }

    public static String desc(Integer value) {
        RuleFeeType ruleFeeType = of(value);
        return ruleFeeType.getDesc();
    }
}
