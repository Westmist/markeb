package org.markeb.hotswap.loader;

import java.io.IOException;
import java.util.Map;

/**
 * 类字节码加载器接口
 */
public interface ClassBytesLoader {

    /**
     * 加载单个类的字节码
     *
     * @param className 全限定类名，如 org.markeb.game.actor.PlayerActorBehavior
     * @return 字节码
     * @throws IOException 加载失败
     */
    byte[] loadClass(String className) throws IOException;

    /**
     * 加载热更包中的所有类
     *
     * @param packagePath 热更包路径
     * @return 类名 -> 字节码 的映射
     * @throws IOException 加载失败
     */
    Map<String, byte[]> loadPackage(String packagePath) throws IOException;

    /**
     * 检查热更包是否存在
     *
     * @param packagePath 热更包路径
     * @return 是否存在
     */
    boolean exists(String packagePath);

}

