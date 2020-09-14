package top.rizon.asyncretryable.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import top.rizon.asyncretryable.annotation.AsyncRetryable;
import top.rizon.asyncretryable.handler.ArgPersistentHandler;
import top.rizon.asyncretryable.handler.EmptyArgHandler;
import top.rizon.asyncretryable.model.BaseTaskParam;
import top.rizon.asyncretryable.model.StatusEnum;
import top.rizon.asyncretryable.model.Task;
import top.rizon.asyncretryable.utils.TaskGeneratorUtils;

/**
 * @author Rizon
 * @date 2020/2/15
 */
@Slf4j
@Aspect
@ConditionalOnProperty(value = "task.enable-async-retry", havingValue = "true")
@Component
@RequiredArgsConstructor
public class AsyncRetryTaskGenerator {
    private final TaskDataHelper dataHelper;
    private final TaskExecuteHelper taskExecuteHelper;
    private final ObjectProvider<ArgPersistentHandler> argPersistentHandler;

    @Pointcut("@annotation(top.rizon.asyncretryable.annotation.AsyncRetryable) && args(top.rizon.asyncretryable.model.BaseTaskParam+)")
    public void methodPointcut() {
    }

    @Around("methodPointcut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        AsyncRetryable retryable = ((MethodSignature) joinPoint.getSignature()).getMethod().getAnnotation(AsyncRetryable.class);
        if (retryable.taskMode()) {
            //采用任务模式
            return doAroundTaskMode(joinPoint);
        }

        Class<? extends Throwable>[] retryExceptions = retryable.retryException();

        //是否是由任务执行器发起的调用
        boolean retryTask = isRetryTask(joinPoint);

        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            //重试任务跳过
            if (retryTask) {
                throw ex;
            }
            //异常类型不是指定的重试异常 跳过
            if (!TaskGeneratorUtils.isIncludeException(ex, retryExceptions)) {
                throw ex;
            }
            log.debug("task failed,submit retry task", ex);
            //生成任务
            submitTask(joinPoint, retryable);
            if (retryable.throwExp()) {
                throw ex;
            } else {
                return null;
            }
        }
    }

    private Object doAroundTaskMode(ProceedingJoinPoint joinPoint) throws Throwable {
        AsyncRetryable retryable = ((MethodSignature) joinPoint.getSignature()).getMethod().getAnnotation(AsyncRetryable.class);

        //是否是由任务执行器发起的调用
        boolean retryTask = isRetryTask(joinPoint);

        if (!retryTask) {
            Task task = submitTask(joinPoint, retryable);
            taskExecuteHelper.tryLockAndExecute(task);
            //对于重试类任务其实不需要返回值
            return null;
        } else {
            return joinPoint.proceed();
        }
    }

    private boolean isRetryTask(ProceedingJoinPoint joinPoint) {
        BaseTaskParam baseTaskParam = (BaseTaskParam) joinPoint.getArgs()[0];
        return baseTaskParam.isRetry();
    }

    private Task submitTask(ProceedingJoinPoint joinPoint, AsyncRetryable annotation) throws IllegalAccessException, InstantiationException {

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Class<?>[] parameterTypes = methodSignature.getMethod().getParameterTypes();

        String tag = annotation.tag();
        if (tag.isEmpty()) {
            tag = methodSignature.getMethod().getName();
        }

        String invokeTargetStr = TaskGeneratorUtils.buildInvokeTarget(joinPoint);

        ArgPersistentHandler handler;
        if (!annotation.argHandler().equals(EmptyArgHandler.class)) {
            handler = annotation.argHandler().newInstance();
        } else {
            handler = this.argPersistentHandler.getIfAvailable();
            if (handler == null) {
                throw new RuntimeException("cannot find ArgPersistentHandler");
            }
        }

        String methodArgStr = handler.serialize(joinPoint.getArgs(), parameterTypes);

        Task task = new Task()
                .setTag(tag)
                .setInvokeTarget(invokeTargetStr)
                .setMethodArgs(methodArgStr)
                .setStatus(StatusEnum.RUNNING.getStatus())
                .setLastTime(System.currentTimeMillis())
                .setProcessing(false);
        dataHelper.saveTask(task);

        log.info("create retry task,id:{},target:{}", task.getId(), invokeTargetStr);
        return task;
    }
}
