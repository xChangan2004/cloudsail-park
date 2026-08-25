package com.changan.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EnergyType implements BaseEnum {
    FUEL(1, "燃油车"),
    NEW_ENERGY(2, "新能源"),
    ;

    @JsonValue
    @EnumValue
    private final int value;
    private final String desc;

    public static EnergyType of(Integer value) {
        if (value == null) {
            return null;
        }
        for (EnergyType energyType : values()) {
            if (energyType.getValue() == value) {
                return energyType;
            }
        }
        return null;
    }

    public static String desc(Integer value) {
        EnergyType energyType = of(value);
        return energyType.getDesc();
    }
}
