package top.rizon.asyncretryable.persistent.datasource;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import top.rizon.asyncretryable.model.StatusEnum;
import top.rizon.asyncretryable.model.Task;
import top.rizon.asyncretryable.sql.CreateTableUtil;
import top.rizon.asyncretryable.task.TaskDataHelper;

import java.util.List;
import java.util.Map;

/**
 * @author Rizon
 * @date 2020/2/16
 */
@RequiredArgsConstructor
public class DsTaskHelper implements TaskDataHelper, InitializingBean {
    private final JdbcTemplate jdbcTemplate;
    private String UNDONE_TASKS_SQL;

    @Override
    public void saveTask(Task task) {

    }

    @Override
    public List<Task> getUndoneTasks() {
        return null;
    }

    @Override
    public void finishTask(long taskId, StatusEnum statusEnum) {

    }

    @Override
    public void updateMethodArgs(long taskId, String methodArgs) {

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //创建表如果不存在
        String sql = CreateTableUtil.createTableSql(Task.class);
        jdbcTemplate.execute(sql);
        UNDONE_TASKS_SQL = CreateTableUtil.selectAsSql(Task.class)
                + " where status = " + StatusEnum.RUNNING.getStatus();
        List<Map<String, Object>> maps = jdbcTemplate.queryForList(UNDONE_TASKS_SQL);
        for (Map<String, Object> map : maps) {
            Task task = new Task();
            BeanUtils.copyProperties(map, task);
            System.out.println("");
        }
    }
}
