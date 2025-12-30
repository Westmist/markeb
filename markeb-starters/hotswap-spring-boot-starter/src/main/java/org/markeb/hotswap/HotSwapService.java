package org.markeb.hotswap;

import org.markeb.hotswap.agent.AgentLoader;
import org.markeb.hotswap.agent.HotSwapAgent;
import org.markeb.hotswap.config.HotSwapProperties;
import org.markeb.hotswap.loader.ClassBytesLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 热更新服务
 * <p>
 * 提供热更新的核心功能。
 * </p>
 */
public class HotSwapService {

    private static final Logger log = LoggerFactory.getLogger(HotSwapService.class);

    private final HotSwapProperties properties;
    private final ClassBytesLoader classLoader;
    private final ApplicationEventPublisher eventPublisher;

    private volatile String currentVersion;

    public HotSwapService(HotSwapProperties properties,
                          ClassBytesLoader classLoader,
                          ApplicationEventPublisher eventPublisher) {
        this.properties = properties;
        this.classLoader = classLoader;
        this.eventPublisher = eventPublisher;
        this.currentVersion = loadVersion();

        // 初始化 Agent
        initAgent();
    }

    /**
     * 初始化 Agent
     */
    private void initAgent() {
        if (!HotSwapAgent.isInitialized()) {
            try {
                AgentLoader.loadAgent();
                log.info("HotSwap agent initialized");
            } catch (Exception e) {
                log.error("Failed to initialize HotSwap agent", e);
            }
        }

        if (!HotSwapAgent.isRedefineClassesSupported()) {
            log.warn("Class redefinition is not supported by this JVM");
        }
    }

    /**
     * 热更新单个类
     *
     * @param className 全限定类名
     * @param bytecode  新的字节码
     * @return 热更新结果
     */
    public HotSwapResult redefineClass(String className, byte[] bytecode) {
        log.info("Redefining class: {}", className);

        try {
            Class<?> clazz = Class.forName(className);
            HotSwapAgent.redefineClass(clazz, bytecode);

            log.info("Class {} redefined successfully", className);
            return HotSwapResult.success(className);

        } catch (ClassNotFoundException e) {
            log.error("Class not found: {}", className, e);
            return HotSwapResult.failure(className, "Class not found: " + e.getMessage());

        } catch (UnsupportedOperationException e) {
            log.error("Class redefinition not supported: {}", className, e);
            return HotSwapResult.failure(className, "Redefinition not supported: " + e.getMessage());

        } catch (Exception e) {
            log.error("Failed to redefine class: {}", className, e);
            return HotSwapResult.failure(className, e.getMessage());
        }
    }

    /**
     * 从 OSS 加载并热更新单个类
     *
     * @param className 全限定类名
     * @return 热更新结果
     */
    public HotSwapResult redefineClassFromOss(String className) {
        try {
            byte[] bytecode = classLoader.loadClass(className);
            return redefineClass(className, bytecode);
        } catch (IOException e) {
            log.error("Failed to load class from OSS: {}", className, e);
            return HotSwapResult.failure(className, "Failed to load from OSS: " + e.getMessage());
        }
    }

    /**
     * 应用热更包
     *
     * @param packagePath 热更包路径
     * @param targetVersion 目标版本号
     * @return 热更新结果列表
     */
    public List<HotSwapResult> applyPackage(String packagePath, String targetVersion) {
        log.info("Applying hotswap package: {}, target version: {}", packagePath, targetVersion);

        List<HotSwapResult> results = new ArrayList<>();

        try {
            // 加载热更包
            Map<String, byte[]> classes = classLoader.loadPackage(packagePath);

            if (classes.isEmpty()) {
                log.warn("No classes found in package: {}", packagePath);
                return results;
            }

            // 逐个热更新
            for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
                HotSwapResult result = redefineClass(entry.getKey(), entry.getValue());
                results.add(result);
            }

            // 统计结果
            long successCount = results.stream().filter(HotSwapResult::isSuccess).count();
            long failCount = results.size() - successCount;

            log.info("Hotswap package applied, success: {}, failed: {}", successCount, failCount);

            // 如果全部成功，更新版本号
            if (failCount == 0 && targetVersion != null) {
                updateVersion(targetVersion);
            }

            // 发布事件
            eventPublisher.publishEvent(new HotSwapEvent(this, results, targetVersion));

        } catch (IOException e) {
            log.error("Failed to apply hotswap package: {}", packagePath, e);
            results.add(HotSwapResult.failure("package", "Failed to load package: " + e.getMessage()));
        }

        return results;
    }

    /**
     * 获取当前版本
     */
    public String getCurrentVersion() {
        return currentVersion;
    }

    /**
     * 更新版本号
     */
    public void updateVersion(String version) {
        this.currentVersion = version;
        saveVersion(version);
        log.info("Version updated to: {}", version);
    }

    /**
     * 从文件加载版本号
     */
    private String loadVersion() {
        try {
            Path versionFile = Path.of(properties.getVersionFile());
            if (Files.exists(versionFile)) {
                return Files.readString(versionFile).trim();
            }
        } catch (IOException e) {
            log.warn("Failed to load version from file", e);
        }
        return properties.getVersion();
    }

    /**
     * 保存版本号到文件
     */
    private void saveVersion(String version) {
        try {
            Path versionFile = Path.of(properties.getVersionFile());
            Files.writeString(versionFile, version);
        } catch (IOException e) {
            log.error("Failed to save version to file", e);
        }
    }

}

