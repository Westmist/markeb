package org.markeb.net.message;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 消息ID注解
 * <p>
 * 用于标注非 Protobuf 消息类的消息ID。
 * Protobuf 消息使用 proto 文件中的 option (msgId) 定义。
 * </p>
 *
 * <pre>
 * {@code
 * @MessageId(12000)
 * public class MyJsonMessage {
 *     private String name;
 *     private int value;
 *     // getters and setters
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MessageId {

    /**
     * 消息ID
     */
    int value();
}

