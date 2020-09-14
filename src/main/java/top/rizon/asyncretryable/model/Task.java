package top.rizon.asyncretryable.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Rizon
 * @date 2020/2/15
 */
@Data
@Accessors(chain = true)
public class Task {
    /**
     * 任务唯一id
     * 保存任务后会生成
     * 用于更新任务数据使用
     */
    private Long id;

    /**
     * 一个自定义的任务分类标示 只用于可读性 不参与代码逻辑
     */
    private String tag;
    /**
     * 要调用的方法的全路径 className;methodName;argTypes
     * eg. {@code top.rizon.WebApplication$TestService;aopTest;java.lang.String,java.lang.String }
     * eg. {@code top.rizon.WebApplication$TestService;aopTest2; }
     */
    private String invokeTarget;
    /**
     * 执行方法的参数，逗号分割的json字符串数组
     */
    private String methodArgs;
    /**
     * 任务状态
     *
     * @see StatusEnum
     */
    private Integer status;

    /**
     * 用于多节点的集群部署时加锁处理
     */
    private long lastTime;

    /**
     * 用于判断锁超时处理
     */
    private boolean processing;

}
