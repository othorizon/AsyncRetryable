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
     * 内部参数不要手动修改
     */
    private boolean isRetry = false;

    /**
     * 除了指定的重试异常之外的失败执行次数
     */
    private int failedCount = 0;
    private long createTime = System.currentTimeMillis();
    /**
     * 下次执行时间
     * 如果时间小于等于当前系统时间则任务会执行
     * 否则会跳过执行
     * 如果需要推迟执行则设置改字段的值
     * <p>
     * 没有该属性的写值操作，可由具体业务方法自己设置
     */
    private long nextExecTime = 0;

    public int incrementFailedCount() {
        return ++failedCount;
    }
}
