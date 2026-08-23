package com.changan.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ParkingSpaceStatus implements BaseEnum {
    FREE(1, "空闲"),
    OCCUPIED(2, "占用"),
    RESERVED(3, "预留"),
    MAINTENANCE(4, "维修");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    public static ParkingSpaceStatus of(Integer value) {
        if (value == null) {
            return null;
        }
        for (ParkingSpaceStatus spaceStatus : values()) {
            if (spaceStatus.getValue() == value) {
                return spaceStatus;
            }
        }
        return null;
    }

    public static String desc(Integer value) {
        ParkingSpaceStatus spaceStatus = of(value);
        return spaceStatus.getDesc();
    }
}
