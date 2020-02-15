package top.rizon.asyncretryable.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import top.rizon.asyncretryable.annotation.AsyncRetryable;
import top.rizon.asyncretryable.handler.ArgPersistentHandler;
import top.rizon.asyncretryable.model.BaseTaskParam;
import top.rizon.asyncretryable.model.StatusEnum;
import top.rizon.asyncretryable.model.Task;
import top.rizon.asyncretryable.utils.TaskGeneratorUtils;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author Rizon
 * @date 2020/2/15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncRetryTaskExecutor implements ApplicationContextAware {
    private final TaskDataHelper dataHelper;
    private ApplicationContext applicationContext;

    @Scheduled(fixedDelay = 10)
    public void processUndoneTasks() {
        List<Task> tasks = dataHelper.getUndoneTasks();
        if (tasks == null) {
            return;
        }
        for (Task task : tasks) {
            try {
                execute(task);
            } catch (Exception ex) {
                log.error("task execute failed", ex);
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
        ArgPersistentHandler argPersistentHandler = annotation.argHandler().newInstance();
        Object[] methodArgs = argPersistentHandler.deserialize(task.getMethodArgs(), invokeMethod.getParameterTypes());

        //修改为重试任务确保不会重复生成任务
        setIsRetry(invokeMethod, methodArgs);

        //执行
        try {
            ReflectionUtils.invokeMethod(invokeMethod, bean, methodArgs);
        } catch (Throwable ex) {
            boolean includeRetryException = TaskGeneratorUtils.isIncludeException(ex, retryExceptions);
            if (includeRetryException) {
                String argsStr = argPersistentHandler.serialize(methodArgs, invokeMethod.getParameterTypes());
                dataHelper.updateMethodArgs(task.getId(), argsStr);
            } else {
                System.out.println("XXXXX");
                dataHelper.finishTask(task.getId(), StatusEnum.FAILED);
            }

        }
    }

    private void setIsRetry(Method invokeMethod, Object[] methodArgs) {
        BaseTaskParam baseTaskParam = (BaseTaskParam)methodArgs[0];
        if (baseTaskParam == null) {
            throw new RuntimeException("cannot find BaseTaskParam for method:" + invokeMethod);
        }
        baseTaskParam.setRetry(true);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
