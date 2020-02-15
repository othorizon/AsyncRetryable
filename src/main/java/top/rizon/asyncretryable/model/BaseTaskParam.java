package top.rizon.asyncretryable.model;

import lombok.Data;

/**
 * @author Rizon
 * @date 2020/2/15
 */
@Data
public abstract class BaseTaskParam {
    /**
     * 是否为重试任务
     */
    private boolean isRetry = false;
}
