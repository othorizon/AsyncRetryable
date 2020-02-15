package top.rizon.asyncretryable.annotation;

import top.rizon.asyncretryable.handler.ArgPersistentHandler;

import java.lang.annotation.*;

/**
 * @author Rizon
 * @date 2020/2/15
 */
@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AsyncRetryable {
    Class<? extends Throwable>[] retryException() default {};

    Class<? extends ArgPersistentHandler> argHandler();

    String tag() default "";

}