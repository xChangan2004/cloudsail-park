package com.changan.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ParkingSpaceType implements BaseEnum {
    NORMAL(1, "普通"),
    NEW_ENERGY(2, "新能源");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    public static ParkingSpaceType of(Integer value) {
        if (value == null) {
            return null;
        }
        for (ParkingSpaceType spaceType : values()) {
            if (spaceType.getValue() == value) {
                return spaceType;
            }
        }
        return null;
    }

    public static String desc(Integer value) {
        ParkingSpaceType spaceType = of(value);
        return spaceType.getDesc();
    }
}
