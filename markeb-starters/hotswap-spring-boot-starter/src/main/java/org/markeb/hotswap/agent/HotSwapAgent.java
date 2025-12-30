package org.markeb.hotswap.agent;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

/**
 * 热更新 Agent
 * <p>
 * 提供 Instrumentation 实例，支持类的热更新。
 * 可以通过启动参数 -javaagent 加载，也可以运行时动态 attach。
 * </p>
 */
public class HotSwapAgent {

    private static volatile Instrumentation instrumentation;

    /**
     * JVM 启动时加载 Agent（-javaagent 方式）
     */
    public static void premain(String args, Instrumentation inst) {
        instrumentation = inst;
    }

    /**
     * 运行时动态 attach Agent
     */
    public static void agentmain(String args, Instrumentation inst) {
        instrumentation = inst;
    }

    /**
     * 获取 Instrumentation 实例
     */
    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }

    /**
     * 设置 Instrumentation 实例（供动态加载使用）
     */
    public static void setInstrumentation(Instrumentation inst) {
        instrumentation = inst;
    }

    /**
     * 检查是否已初始化
     */
    public static boolean isInitialized() {
        return instrumentation != null;
    }

    /**
     * 重新定义类
     *
     * @param clazz    要重新定义的类
     * @param bytecode 新的字节码
     * @throws ClassNotFoundException      类未找到
     * @throws UnmodifiableClassException  类不可修改
     */
    public static void redefineClass(Class<?> clazz, byte[] bytecode)
            throws ClassNotFoundException, UnmodifiableClassException {
        if (instrumentation == null) {
            throw new IllegalStateException("Instrumentation not initialized. " +
                    "Please ensure the agent is loaded via -javaagent or dynamic attach.");
        }
        instrumentation.redefineClasses(new ClassDefinition(clazz, bytecode));
    }

    /**
     * 检查是否支持类重定义
     */
    public static boolean isRedefineClassesSupported() {
        return instrumentation != null && instrumentation.isRedefineClassesSupported();
    }

}

