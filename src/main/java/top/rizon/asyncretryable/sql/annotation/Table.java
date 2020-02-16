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
public @interface Table {
    String tableName() default ""; //默认表名为空
    String keyFields() default "id"; //默认主键为id
}
