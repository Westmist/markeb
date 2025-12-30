package org.markeb.hotswap.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Agent 动态加载器
 * <p>
 * 支持在运行时动态加载 Agent，无需启动参数。
 * </p>
 */
public class AgentLoader {

    private static final Logger log = LoggerFactory.getLogger(AgentLoader.class);

    private static volatile boolean loaded = false;

    /**
     * 动态加载 Agent
     * <p>
     * 如果已经通过 -javaagent 加载，则跳过。
     * </p>
     */
    public static synchronized void loadAgent() {
        if (HotSwapAgent.isInitialized()) {
            log.info("Agent already initialized via -javaagent");
            return;
        }

        if (loaded) {
            log.info("Agent already loaded dynamically");
            return;
        }

        try {
            String pid = getProcessId();
            File agentJar = createAgentJar();

            log.info("Loading agent dynamically, pid: {}, agent jar: {}", pid, agentJar.getAbsolutePath());

            // 使用反射调用 VirtualMachine，避免编译时依赖
            Class<?> vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
            Object vm = vmClass.getMethod("attach", String.class).invoke(null, pid);

            try {
                vmClass.getMethod("loadAgent", String.class).invoke(vm, agentJar.getAbsolutePath());
                loaded = true;
                log.info("Agent loaded successfully");
            } finally {
                vmClass.getMethod("detach").invoke(vm);
            }

            // 清理临时文件
            agentJar.deleteOnExit();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load agent dynamically", e);
        }
    }

    /**
     * 获取当前进程 ID
     */
    private static String getProcessId() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return name.split("@")[0];
    }

    /**
     * 创建临时 Agent JAR 文件
     */
    private static File createAgentJar() throws IOException {
        File jarFile = File.createTempFile("hotswap-agent-", ".jar");

        Manifest manifest = new Manifest();
        Attributes attrs = manifest.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attrs.putValue("Agent-Class", HotSwapAgent.class.getName());
        attrs.putValue("Premain-Class", HotSwapAgent.class.getName());
        attrs.putValue("Can-Redefine-Classes", "true");
        attrs.putValue("Can-Retransform-Classes", "true");

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile), manifest)) {
            // 添加 HotSwapAgent.class 到 JAR
            String classPath = HotSwapAgent.class.getName().replace('.', '/') + ".class";
            jos.putNextEntry(new ZipEntry(classPath));

            try (InputStream is = HotSwapAgent.class.getClassLoader().getResourceAsStream(classPath)) {
                if (is != null) {
                    is.transferTo(jos);
                }
            }
            jos.closeEntry();
        }

        return jarFile;
    }

}

