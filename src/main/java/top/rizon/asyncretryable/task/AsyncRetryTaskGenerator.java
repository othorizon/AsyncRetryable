package top.rizon.asyncretryable.task;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import top.rizon.asyncretryable.annotation.AsyncRetryable;
import top.rizon.asyncretryable.handler.ArgPersistentHandler;
import top.rizon.asyncretryable.model.BaseTaskParam;
import top.rizon.asyncretryable.model.StatusEnum;
import top.rizon.asyncretryable.model.Task;
import top.rizon.asyncretryable.utils.TaskGeneratorUtils;

/**
 * @author Rizon
 * @date 2020/2/15
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AsyncRetryTaskGenerator {
    private final TaskDataHelper dataHelper;

    @Pointcut("@annotation(top.rizon.asyncretryable.annotation.AsyncRetryable) && args(top.rizon.asyncretryable.model.BaseTaskParam+)")
    public void methodPointcut() {
    }

    @Around("methodPointcut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        AsyncRetryable retryable = ((MethodSignature) joinPoint.getSignature()).getMethod().getAnnotation(AsyncRetryable.class);
        Class<? extends Throwable>[] retryExceptions = retryable.retryException();

        boolean retryTask = isRetryTask(joinPoint);


        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            //跳过重试任务
            if (!retryTask
                    //包含重试的异常
                    && TaskGeneratorUtils.isIncludeException(ex, retryExceptions)) {
                submitTask(joinPoint, retryable);
            }
            throw ex;
        }
    }

    private boolean isRetryTask(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        BaseTaskParam baseTaskParam = (BaseTaskParam) joinPoint.getArgs()[0];
        return baseTaskParam.isRetry();
    }

    private void submitTask(ProceedingJoinPoint joinPoint, AsyncRetryable annotation) throws IllegalAccessException, InstantiationException {
        Class<? extends ArgPersistentHandler> argHandler = annotation.argHandler();
        ArgPersistentHandler argPersistentHandler = argHandler.newInstance();

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Class<?>[] parameterTypes = methodSignature.getMethod().getParameterTypes();

        String tag = annotation.tag();
        String invokeTargetStr = TaskGeneratorUtils.buildInvokeTarget(joinPoint);
        String methodArgStr = argPersistentHandler.serialize(joinPoint.getArgs(), parameterTypes);

        Task task = new Task()
                .setTag(tag)
                .setInvokeTarget(invokeTargetStr)
                .setMethodArgs(methodArgStr)
                .setStatus(StatusEnum.RUNNING.getStatus());
        dataHelper.saveTask(task);
    }
}
