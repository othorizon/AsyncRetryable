package top.rizon.asyncretryable.handler;

/**
 * @author Rizon
 * @date 2020/2/16
 */
public class EmptyArgHandler implements ArgPersistentHandler {
    @Override
    public String serialize(Object[] args, Class<?>[] parameterTypes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] deserialize(String args, Class<?>[] parameterTypes) {
        throw new UnsupportedOperationException();
    }
}
