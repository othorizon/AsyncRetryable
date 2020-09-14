package top.rizon.asyncretryable.task;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import top.rizon.asyncretryable.annotation.AsyncRetryable;
import top.rizon.asyncretryable.handler.ArgPersistentHandler;
import top.rizon.asyncretryable.handler.EmptyArgHandler;
import top.rizon.asyncretryable.model.BaseTaskParam;
import top.rizon.asyncretryable.model.StatusEnum;
import top.rizon.asyncretryable.model.Task;
import top.rizon.asyncretryable.model.TaskRetryEvent;
import top.rizon.asyncretryable.utils.TaskGeneratorUtils;

import java.lang.reflect.Method;

/**
 * @author Rizon
 * @date 2020/3/25
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskExecuteHelper implements ApplicationContextAware {
    private final TaskDataHelper dataHelper;
    private final ObjectProvider<ArgPersistentHandler> argPersistentHandler;
    private ApplicationContext applicationContext;

    @Value("${task.lock-timeout}")
    @Setter
    private Integer lockTimeout = 30000;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 尝试获取锁并执行任务
     */
    void tryLockAndExecute(Task task) {
        boolean locked = false;
        try {
            log.info("process task {}", task.getId());
            locked = tryLock(task);
            if (!locked) {
                log.info("task {} get lock failed,skip", task.getId());
                return;
            }

            execute(task);
        } catch (Exception ex) {
            log.error("task " + task.getId() + "process failed", ex);
        } finally {
            if (locked) {
                releaseLock(task);
            }
        }
    }

    private void execute(Task task) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        TaskGeneratorUtils.InvokeProp invokeProp = TaskGeneratorUtils.getInvokeProp(task.getInvokeTarget());
        Class<?> invokeClass = Class.forName(invokeProp.getClassName());
        Object bean = applicationContext.getBean(invokeClass);

        Method invokeMethod = ReflectionUtils.findMethod(bean.getClass(),
                invokeProp.getMethodName(), invokeProp.getParamTypes());
        if (invokeMethod == null) {
            throw new RuntimeException("cannot find invoke method:" + invokeProp);
        }


        AsyncRetryable annotation = AnnotationUtils.findAnnotation(invokeMethod, AsyncRetryable.class);
        if (annotation == null) {
            throw new RuntimeException("cannot find AsyncRetryable annotation for method:" + invokeProp);
        }
        Class<? extends Throwable>[] retryExceptions = annotation.retryException();

        ArgPersistentHandler handler;
        if (!annotation.argHandler().equals(EmptyArgHandler.class)) {
            handler = annotation.argHandler().newInstance();
        } else {
            handler = this.argPersistentHandler.getIfAvailable();
            if (handler == null) {
                throw new RuntimeException("cannot find ArgPersistentHandler");
            }
        }
        Object[] methodArgs = handler.deserialize(task.getMethodArgs(), invokeMethod.getParameterTypes());

        BaseTaskParam baseTaskParam = (BaseTaskParam) methodArgs[0];
        if (baseTaskParam == null) {
            throw new RuntimeException("cannot find BaseTaskParam for method:" + invokeMethod);
        }
        //没有达到执行时间则不执行
        if (baseTaskParam.getNextExecTime() > System.currentTimeMillis()) {
            log.info("task {} delayed,next execute time:{}", task.getId(), baseTaskParam.getNextExecTime());
            return;
        }

        //修改为重试任务确保不会重复生成任务
        baseTaskParam.setRetry(true);
        //发布重试任务事件
        applicationContext.publishEvent(new TaskRetryEvent(this, methodArgs));

        //执行
        try {
            ReflectionUtils.invokeMethod(invokeMethod, bean, methodArgs);
        } catch (Throwable ex) {
            boolean includeRetryException = TaskGeneratorUtils.isIncludeException(ex, retryExceptions);
            if (includeRetryException) {
                log.info("task " + task.getId() + " process result is failed,wait next retry", ex);
                String argsStr = handler.serialize(methodArgs, invokeMethod.getParameterTypes());
                dataHelper.updateMethodArgs(task.getId(), argsStr);
            } else {
                log.error("task " + task.getId() + " process result is failed,task failed", ex);
                dataHelper.finishTask(task.getId(), StatusEnum.FAILED);
            }
            return;
        }
        log.info("task {} process result is success", task.getId());
        dataHelper.finishTask(task.getId(), StatusEnum.SUCCESS);
    }


    private void releaseLock(Task task) {
        int result = dataHelper.updateProcessing(task.getId(), task.getLastTime(),
                false, System.currentTimeMillis());
        if (result < 1) {
            //解锁在任务超时的情况下会失败 失败则忽略即可 没有回滚处理
            log.info("task {} unlock failed,maybe timeout", task.getId());
        }
    }

    private boolean tryLock(Task task) {
        boolean forceLock = false;
        //锁超时处理
        if (task.isProcessing()) {
            if (System.currentTimeMillis() > task.getLastTime() + lockTimeout) {
                log.info("task {} lock timeout,force lock", task.getId());
                forceLock = true;
            } else {
                log.info("task {} is processing,lock failed", task.getId());
                return false;
            }
        }
        //如果强制锁则时间为null
        Long lockTime = forceLock ? null : task.getLastTime();
        //生成新的时间戳
        task.setLastTime(System.currentTimeMillis());
        int result = dataHelper.updateProcessing(task.getId(), lockTime,
                true, task.getLastTime());
        return result > 0;
    }
}
