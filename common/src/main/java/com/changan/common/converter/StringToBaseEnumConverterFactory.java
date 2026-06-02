package com.changan.common.converter;

import com.changan.common.enums.BaseEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

public class StringToBaseEnumConverterFactory implements ConverterFactory<String, BaseEnum> {
    @Override
    public <T extends BaseEnum> Converter<String, T> getConverter(Class<T> targetType) {
        return new Converter<String, T>() {
            @Override
            public T convert(String value) {
                T[] enumConstants = targetType.getEnumConstants();
                int intValue;
                try {
                    intValue = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("参数值'" + value + "'无法转换为枚举类型" + targetType.getSimpleName());
                }
                for (T enumConstant : enumConstants) {
                    if (enumConstant.equalsValue(intValue)) {
                        return enumConstant;
                    }
                }
                throw new IllegalArgumentException("枚举" + targetType.getSimpleName() + "不存在值" + value);
            }
        };
    }
}
