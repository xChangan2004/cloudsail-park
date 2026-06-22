package com.changan.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Sex implements BaseEnum {

    FEMALE(0, "女"),
    MALE(1, "男"),
    ;

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    public static Sex of(Integer value) {
        if (value == null) {
            return null;
        }
        for (Sex sex : values()) {
            if (sex.getValue() == value) {
                return sex;
            }
        }
        return null;
    }

    public static String desc(Integer value) {
        Sex sex = of(value);
        return sex.getDesc();
    }
}
