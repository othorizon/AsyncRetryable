package top.rizon.asyncretryable.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import top.rizon.asyncretryable.annotation.AsyncRetryable;
import top.rizon.asyncretryable.handler.ArgPersistentHandler;
import top.rizon.asyncretryable.handler.EmptyArgHandler;
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
    private final ObjectProvider<ArgPersistentHandler> argPersistentHandler;
    private ApplicationContext applicationContext;

    @Scheduled(fixedDelay = 60000)
    public void processUndoneTasks() {
        log.info("find undone tasks");
        List<Task> tasks = dataHelper.getUndoneTasks();
        if (tasks == null) {
            log.info("undone tasks is null");
            return;
        }
        log.info("undone tasks size:" + tasks.size());
        for (Task task : tasks) {
            try {
                log.info("process task {}", task.getId());
                execute(task);
            } catch (Exception ex) {
                log.error("task " + task.getId() + "process failed", ex);
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

        //修改为重试任务确保不会重复生成任务
        setIsRetry(invokeMethod, methodArgs);

        //执行
        try {
            ReflectionUtils.invokeMethod(invokeMethod, bean, methodArgs);
        } catch (Throwable ex) {
            boolean includeRetryException = TaskGeneratorUtils.isIncludeException(ex, retryExceptions);
            if (includeRetryException) {
                log.info("task {} process result is failed,wait next retry:{}", task.getId(), ex.getMessage());
                String argsStr = handler.serialize(methodArgs, invokeMethod.getParameterTypes());
                dataHelper.updateMethodArgs(task.getId(), argsStr);
            } else {
                log.info("task {} process result is failed,task failed:{}", task.getId(), ex.getMessage());
                dataHelper.finishTask(task.getId(), StatusEnum.FAILED);
            }
            return;
        }
        log.info("task {} process result is success", task.getId());
        dataHelper.finishTask(task.getId(), StatusEnum.SUCCESS);
    }

    private void setIsRetry(Method invokeMethod, Object[] methodArgs) {
        BaseTaskParam baseTaskParam = (BaseTaskParam) methodArgs[0];
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
