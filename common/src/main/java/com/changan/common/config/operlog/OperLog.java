package com.changan.common.config.operlog;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperLog {

    String type();  // 操作模块

    String subType(); // 操作名

    boolean recordParams() default true; // 记录参数
}
