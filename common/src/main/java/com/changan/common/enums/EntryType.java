package com.changan.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EntryType implements BaseEnum {
    MANUAL(1, "手动"),
    OCR(2, "OCR识别"),
    ;

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    public static EntryType of(Integer value) {
        if (value == null) {
            return null;
        }
        for (EntryType entryType : EntryType.values()) {
            if (entryType.getValue() == value) {
                return entryType;
            }
        }
        return null;
    }

    public static String desc(Integer value) {
        EntryType entryType = of(value);
        return entryType.getDesc();
    }
}
