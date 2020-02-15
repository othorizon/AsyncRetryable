package top.rizon.asyncretryable.task;

import top.rizon.asyncretryable.model.StatusEnum;
import top.rizon.asyncretryable.model.Task;

import java.util.List;

/**
 * @author Rizon
 * @date 2020/2/15
 */
public interface TaskDataHelper {
    /**
     * 保存任务
     * @param task
     */
    void saveTask(Task task);

    /**
     * 获取所有未完成的任务
     * @return
     */
    List<Task> getUndoneTasks();

    /**
     * 更新任务状态
     * @param taskId 任务唯一id
     * @param statusEnum
     */
    void finishTask(long taskId, StatusEnum statusEnum);

    /**
     * 更新参数数据
     * @param taskId 任务唯一id
     * @param methodArgs
     */
    void updateMethodArgs(long taskId, String methodArgs);
}
