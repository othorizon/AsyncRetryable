package top.rizon.asyncretryable.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.rizon.asyncretryable.model.Task;

import java.util.List;

/**
 * @author Rizon
 * @date 2020/2/15
 */
@Slf4j
@ConditionalOnProperty(value = "task.enable-async-retry", havingValue = "true")
@Component
@RequiredArgsConstructor
public class AsyncRetryTaskExecutor {
    private final TaskDataHelper dataHelper;
    private final TaskExecuteHelper taskExecuteHelper;

    @Scheduled(fixedDelayString = "${task.fixed-delay}")
    public void processUndoneTasks() {
        log.info("find undone tasks");
        List<Task> tasks = dataHelper.getUndoneTasks();
        if (tasks == null) {
            log.info("undone tasks is empty");
            return;
        }
        log.info("undone tasks size:" + tasks.size());
        for (Task task : tasks) {
            taskExecuteHelper.tryLockAndExecute(task);
        }
    }

}
