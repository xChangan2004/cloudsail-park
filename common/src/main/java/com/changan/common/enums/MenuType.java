package com.changan.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MenuType implements BaseEnum {

    CATALOGUE(1, "目录"),
    MENU(2, "菜单"),
    BUTTON(3, "按钮"),
    ;

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    public static MenuType of(Integer value) {
        if (value == null) {
            return null;
        }
        for (MenuType menuType : values()) {
            if (menuType.getValue() == value) {
                return menuType;
            }
        }
        return null;
    }

    public static String desc(Integer value) {
        MenuType menuType = of(value);
        return menuType.getDesc();
    }
}
