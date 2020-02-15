package top.rizon.asyncretryable.handler;

/**
 * 参数持久化处理工具
 *
 * @author Rizon
 * @date 2020/2/15
 */
public interface ArgPersistentHandler {
    String serialize(Object[] args, Class<?>[] parameterTypes);

    Object[] deserialize(String args, Class<?>[] parameterTypes);
}
