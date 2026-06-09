package com.campus.service.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD) // 标记这个方法需要记录日志
@Retention(RetentionPolicy.RUNTIME)
public @interface OpLog {
    String value() default "";
}
