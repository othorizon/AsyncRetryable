package top.rizon.asyncretryable.sql.annotation;

import java.lang.annotation.*;

/**
 * @author Rizon
 * @date 2020/2/16
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface Column {
    String name() default ""; //表示字段名
}
