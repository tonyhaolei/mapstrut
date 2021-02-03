package com.example.mapstrut.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TODO
 *
 * @author lei.hao
 * @date 2021/1/25
 */
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Inherited
public @interface MapStructGenerate {
    boolean isGenerateAll() default false;

    String expandMethod() default "";
}
