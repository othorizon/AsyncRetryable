package top.rizon;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import top.rizon.asyncretryable.annotation.AsyncRetryable;
import top.rizon.asyncretryable.handler.ArgPersistentHandler;
import top.rizon.asyncretryable.model.BaseTaskParam;
import top.rizon.asyncretryable.task.AsyncRetryTaskExecutor;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Rizon
 * @date 2019/8/12
 */
@EnableScheduling
@SpringBootApplication()
public class WebApplication implements ApplicationContextAware {

    @Autowired
    TestService testService;
    @Autowired
    AsyncRetryTaskExecutor asyncRetryTaskExecutor;
    private ApplicationContext applicationContext;

    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }

    @PostConstruct
    public void test() throws ClassNotFoundException, InterruptedException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException, SQLException {
//        try {
//            testService.aopTest("123", "456");
//        } catch (Exception ex) {
//            System.out.println(ex);
//        }
//        try {
//            testService.aopTest2();
//        } catch (Exception ex) {
//            System.out.println(ex);
//        }
//        test2();
        try {
            testService.aopTest3(null, null);
            testService.aopTest4(new Test4Param());
        } catch (Exception ex) {

        }
    }

    public void test2() throws ClassNotFoundException {
        Class<?> clazz = Class.forName("top.rizon.WebApplication$TestService");
        Object bean = applicationContext.getBean(clazz);
        Class<?>[] paramClass = new Class[2];
        paramClass[0] = String.class;
        paramClass[1] = String.class;
        Method aopTest = ReflectionUtils.findMethod(bean.getClass(), "aopTest", paramClass);
        Object[] params = new Object[]{"123", "456"};
        try {
            ReflectionUtils.invokeMethod(aopTest, bean, params);
        } catch (Exception ex) {
            System.out.println("");
        }
        System.out.println("xxx");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Service
    public static class TestService {
        @AsyncRetryable(argHandler = StringArgHandler.class, retryException = Exception.class)
        public void aopTest(String arg1, String arg2) {
            System.out.println(arg2);
            throw new RuntimeException(arg1);
        }

        @AsyncRetryable(argHandler = StringArgHandler.class, retryException = Exception.class)
        public void aopTest2() {
            System.out.println("123");
            throw new RuntimeException("123");
        }

        @AsyncRetryable(argHandler = StringArgHandler.class, retryException = Exception.class)
        public void aopTest3(String task, String[] taskId) {
            System.out.println(task);
            System.out.println(taskId);
        }

        @AsyncRetryable(argHandler = StringArgHandler.class, retryException = IllegalArgumentException.class)
        public void aopTest4(Test4Param param) {
            System.out.println("aopTest4:" + param.getCount());
            param.setCount(param.getCount() + 1);
            if (param.getCount() < 3) {
                throw new IllegalArgumentException("retry:" + param.getCount());
            } else {
                throw new RuntimeException("fail:" + param.getCount());
            }
        }

    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class Test4Param extends BaseTaskParam {
        private int count;
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
