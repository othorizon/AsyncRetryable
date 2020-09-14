package top.rizon;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import top.rizon.asyncretryable.model.StatusEnum;
import top.rizon.asyncretryable.model.Task;
import top.rizon.asyncretryable.task.TaskDataHelper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 存储在内存中的任务
 * 模拟持久化到Mysql等数据源中的操作
 *
 * @author Rizon
 * @date 2020/2/15
 */
@Slf4j
public class MemTaskHelper implements TaskDataHelper {
    private final AtomicLong id = new AtomicLong();
    private final Map<Long, Task> tasks = new ConcurrentHashMap<>();

    @Override
    public void saveTask(Task task) {
        //持久化
        Task persistence = cloneTask(task);
        persistence.setId(id.incrementAndGet());
        tasks.put(persistence.getId(), persistence);
        //回写任务ID
        task.setId(persistence.getId());
    }

    @Override
    public List<Task> getUndoneTasks() {
        return tasks.values().stream()
                .filter(t -> StatusEnum.RUNNING.getStatus().equals(t.getStatus()))
                .map(MemTaskHelper::cloneTask)
                .collect(Collectors.toList());
    }

    /**
     * 因为是内存对象操作，所以clone一个对象避免副作用
     */
    private static Task cloneTask(Task task) {
        Task newTask = new Task();
        BeanUtils.copyProperties(task, newTask);
        return newTask;
    }

    @Override
    public void finishTask(long taskId, StatusEnum statusEnum) {
        log.info("finish task,taskId:{},status:{}", taskId, statusEnum.name());
        tasks.get(taskId).setStatus(statusEnum.getStatus());
    }

    @Override
    public void updateMethodArgs(long taskId, String methodArgs) {
        tasks.get(taskId).setMethodArgs(methodArgs);
    }

    @Override
    public int updateProcessing(long taskId, Long lastTime, boolean processing, long processTime) {
        Task task = tasks.get(taskId);
        //lastTime是用于乐观锁的处理
        if (task.getLastTime() != lastTime) {
            return 0;
        }
        task.setProcessing(processing).setLastTime(processTime);
        return 1;
    }

}
