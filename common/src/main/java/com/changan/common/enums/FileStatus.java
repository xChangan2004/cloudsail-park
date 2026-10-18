package com.changan.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileStatus implements BaseEnum {

    TEMP(0, "临时"),
    USED(1, "已使用");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    public static FileStatus of(Integer value) {
        if (value == null) {
            return null;
        }
        for (FileStatus status : FileStatus.values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        return null;
    }
}
