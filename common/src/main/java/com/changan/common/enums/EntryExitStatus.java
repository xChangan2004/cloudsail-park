package com.changan.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EntryExitStatus implements BaseEnum {
    PENDING_ENTRY(0, "待入场"),
    ENTERED(1, "已入场"),
    EXITED(2, "已离场"),
    ;

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    public static EntryExitStatus of(Integer value) {
        if (value == null) {
            return null;
        }
        for (EntryExitStatus status : EntryExitStatus.values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        return null;
    }

    public static String desc(Integer value) {
        EntryExitStatus status = of(value);
        return status.getDesc();
    }
}
