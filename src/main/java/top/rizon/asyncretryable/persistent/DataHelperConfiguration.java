package top.rizon.asyncretryable.persistent;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import top.rizon.asyncretryable.persistent.datasource.DsTaskHelper;
import top.rizon.asyncretryable.task.TaskDataHelper;

import javax.sql.DataSource;

/**
 * @author Rizon
 * @date 2020/2/16
 */
//@Configuration
@EnableConfigurationProperties(DataSourceProperties.class)
@RequiredArgsConstructor
public class DataHelperConfiguration {
    private final ObjectProvider<DataSourceProperties> dataSourceProperties;
    private final ObjectProvider<DataSource> dataSource;

    @Bean
    public TaskDataHelper taskDataHelper() {
        DataSourceProperties dsProperties;
        try {
            dsProperties = dataSourceProperties.getIfAvailable();
        } catch (Exception ex) {
            dsProperties = null;
        }

        if (dsProperties != null &&
                !StringUtils.isEmpty(dsProperties.getUrl())
                && dataSource.getIfUnique() != null) {

            return new DsTaskHelper(new JdbcTemplate(dataSource.getIfUnique()));
        }
        return new MemTaskHelper();
    }


}
