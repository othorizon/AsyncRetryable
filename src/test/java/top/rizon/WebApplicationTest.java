package top.rizon;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import top.rizon.asyncretryable.annotation.AsyncRetryable;
import top.rizon.asyncretryable.handler.ArgPersistentHandler;
import top.rizon.asyncretryable.model.BaseTaskParam;
import top.rizon.asyncretryable.task.AsyncRetryTaskExecutor;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Rizon
 * @date 2019/8/12
 */
@Slf4j
@EnableScheduling
@SpringBootApplication()
public class WebApplicationTest {

    @Autowired
    TestService testService;
    @Autowired
    AsyncRetryTaskExecutor asyncRetryTaskExecutor;
    /**
     * 推迟多少毫秒后执行
     */
    private static int maxRetryCount;
    private static int delayMillis;

    @Value("${task.max-retry-count}")
    public void setMaxRetryCount(Integer maxRetryCount) {
        WebApplicationTest.maxRetryCount = maxRetryCount;
    }

    @Value("${task.delay-millis}")
    public void setDelayTime(int delayMillis) {
        WebApplicationTest.delayMillis = delayMillis;
    }


    public static void main(String[] args) {
        SpringApplication.run(WebApplicationTest.class, args);
    }


    /**
     * 指定一个任务持久化方案
     */
    @Bean
    public MemTaskHelper memTaskHelper() {
        return new MemTaskHelper();
    }

    @PostConstruct
    public void test() {
        log.info("start test");
        testService.failTest(new TestParam(System.currentTimeMillis()));
    }


    @Service
    public static class TestService {

        @AsyncRetryable(argHandler = StringArgHandler.class, retryException = RetryException.class)
        public void failTest(TestParam param) {
            log.info("method params:{}", param);
            if (param.incrementFailedCount() < maxRetryCount) {
                //失败之后延迟一定时间之后再触发重试
                param.setNextExecTime(System.currentTimeMillis() + delayMillis);
                throw new RetryException(String.format("retry:%s,maxRetryCount:%s", param.getFailedCount(), maxRetryCount));
            } else {
                throw new RuntimeException("fail:" + param.getFailedCount());
            }
        }
    }

    public static class RetryException extends RuntimeException {
        /**
         * 仅包含message, 没有cause, 也不记录栈异常, 性能最高
         *
         * @param msg
         */
        public RetryException(String msg) {
            this(msg, false);
        }

        /**
         * 包含message和cause, 会记录栈异常
         *
         * @param msg
         * @param cause
         */
        public RetryException(String msg, Throwable cause) {
            super(msg, cause, false, true);
        }

        /**
         * 包含message, 可指定是否记录异常
         *
         * @param msg
         * @param recordStackTrace
         */
        public RetryException(String msg, boolean recordStackTrace) {
            super(msg, null, false, recordStackTrace);
        }

    }


    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    @Data
    @AllArgsConstructor
    public static class TestParam extends BaseTaskParam {
        private Long timeTag;
    }

    public static class StringArgHandler implements ArgPersistentHandler {

        @Override
        public String serialize(Object[] args, Class<?>[] parameterTypes) {
            if (args == null) {
                return null;
            }
            Gson gson = new Gson();
            List<String> jsonList = new ArrayList<>();
            for (Object arg : args) {
                jsonList.add(gson.toJson(arg));
            }
            return gson.toJson(jsonList);
        }

        @Override
        public Object[] deserialize(String args, Class<?>[] parameterTypes) {
            if (args == null) {
                return null;
            }
            Object[] result;
            Gson gson = new Gson();
            List<String> jsonList = gson.fromJson(args, new TypeToken<ArrayList<String>>() {
            }.getType());
            if (jsonList.size() != parameterTypes.length) {
                throw new RuntimeException("parameterTypes size not equals args size");
            }
            result = new Object[jsonList.size()];
            for (int i = 0; i < jsonList.size(); i++) {
                result[i] = gson.fromJson(jsonList.get(i), parameterTypes[i]);
            }
            return result;
        }
    }
}
