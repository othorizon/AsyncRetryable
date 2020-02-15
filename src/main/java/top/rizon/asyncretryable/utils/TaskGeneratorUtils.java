package top.rizon.asyncretryable.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Rizon
 * @date 2020/2/15
 */
public class TaskGeneratorUtils {


    public static String buildInvokeTarget(ProceedingJoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        String classFullName = methodSignature.getDeclaringTypeName();
        String methodName = method.getName();
        String argTypes = getParamTypesStr(method.getParameterTypes());
        return classFullName + ";" + methodName + ";" + argTypes;
    }

    public static InvokeProp getInvokeProp(@NonNull String invokeTarget) throws ClassNotFoundException {
        String[] split = invokeTarget.split(";");
        String className = split[0];
        String methodName = split[1];
        Class<?>[] paramTypes = null;
        if (split.length == 3) {
            paramTypes = getParamTypes(split[2]);
        }
        return new InvokeProp(className, methodName, paramTypes);
    }


    private static String getParamTypesStr(Class<?>[] clazz) {
        if (clazz == null) {
            return "";
        }
        return Arrays.stream(clazz)
                .map(Class::getName)
                .collect(Collectors.joining(","));
    }

    private static Class<?>[] getParamTypes(String argTypesStr) throws ClassNotFoundException {
        Class<?>[] types;
        String[] split = argTypesStr.split(",");
        types = new Class[split.length];
        for (int i = 0; i < split.length; i++) {
            types[i] = Class.forName(split[i]);
        }
        return types;
    }

    public static boolean isIncludeException(Throwable ex, Class<? extends Throwable>[] retryExceptions) {
        for (Class<? extends Throwable> retryException : retryExceptions) {
            if (retryException.isAssignableFrom(ex.getClass())) {
                return true;
            }
        }
        return false;
    }

    @Data
    @AllArgsConstructor
    public static class InvokeProp {
        private String className;
        private String methodName;
        private Class<?>[] paramTypes;
    }

}
