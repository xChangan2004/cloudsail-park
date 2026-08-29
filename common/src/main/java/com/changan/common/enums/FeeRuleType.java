package com.changan.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FeeRuleType implements BaseEnum {
    STANDARD(1, "标准"),
    PERIOD(2, "时段"),
    TIERED(3, "阶梯");

    @JsonValue
    @EnumValue
    private final int value;
    private final String desc;

    public static FeeRuleType of(Integer value) {
        if (value == null) {
            return null;
        }
        for (FeeRuleType feeRuleType : values()) {
            if (feeRuleType.getValue() == value) {
                return feeRuleType;
            }
        }
        return null;
    }

    public static String desc(Integer value) {
        FeeRuleType feeRuleType = of(value);
        return feeRuleType.getDesc();
    }
}
