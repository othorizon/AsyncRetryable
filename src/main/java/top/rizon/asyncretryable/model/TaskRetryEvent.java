package top.rizon.asyncretryable.model;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author Rizon
 * @date 2020/3/20
 */
public class TaskRetryEvent extends ApplicationEvent {
    @Getter
    private Object[] methodArgs;

    public TaskRetryEvent(Object source,Object[] methodArgs) {
        super(source);
        this.methodArgs = methodArgs;
    }
}
