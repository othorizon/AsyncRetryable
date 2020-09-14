package top.rizon.asyncretryable.annotation;


import top.rizon.asyncretryable.handler.ArgPersistentHandler;
import top.rizon.asyncretryable.handler.EmptyArgHandler;

import java.lang.annotation.*;

/**
 * @author Rizon
 * @date 2020/2/15
 */
@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AsyncRetryable {
    /**
     * 如果方法抛出该异常则会创建重试任务
     */
    Class<? extends Throwable>[] retryException() default {};

    /**
     * 参数序列化的类
     */
    Class<? extends ArgPersistentHandler> argHandler() default EmptyArgHandler.class;

    String tag() default "";

    /**
     * 如果生成了重试任务是否还会继续抛出异常
     */
    boolean throwExp() default false;

    /**
     * 任务模式会先生成任务再去执行
     * 目前任务模式下会忽略方法的返回值 即如果方法有返回值一定会返回null
     * 非任务模式下只有在执行失败需要重试的时候才会生成任务
     */
    boolean taskMode() default true;

}
