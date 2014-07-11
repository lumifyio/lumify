package io.lumify.gdelt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface GDELTField {
    String name();
    boolean required() default false;
    String dateFormat() default "";
}
