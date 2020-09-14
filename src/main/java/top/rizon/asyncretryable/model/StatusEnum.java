package top.rizon.asyncretryable.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Rizon
 * @date 2020/2/15
 */
@AllArgsConstructor
@Getter
public enum StatusEnum {
    /*默认状态*/
    RUNNING(0),
    SUCCESS(1),
    FAILED(2),
    /*等待手动执行*/
    PENDING(3)
    ;
    private Integer status;
}
