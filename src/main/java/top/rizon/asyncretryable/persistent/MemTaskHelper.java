package top.rizon.asyncretryable.persistent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import top.rizon.asyncretryable.model.StatusEnum;
import top.rizon.asyncretryable.model.Task;
import top.rizon.asyncretryable.task.TaskDataHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 存储在内存中的任务持久化
 *
 * @author Rizon
 * @date 2020/2/15
 */
@Slf4j
@Component
@ConditionalOnBean(TaskDataHelper.class)
public class MemTaskHelper implements TaskDataHelper {
    private final AtomicLong id = new AtomicLong();
    private final Map<Long, Task> tasks = new ConcurrentHashMap<>();

    @Override
    public void saveTask(Task task) {
        task.setId(id.incrementAndGet());
        tasks.put(task.getId(), task);
    }

    @Override
    public List<Task> getUndoneTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public void finishTask(long taskId, StatusEnum statusEnum) {
        log.debug("finish task,taskId:{},status:{}", taskId, statusEnum.name());
        tasks.remove(taskId);
    }

    @Override
    public void updateMethodArgs(long taskId, String methodArgs) {
        tasks.get(taskId).setMethodArgs(methodArgs);
    }

}
