package org.markeb.hotswap.script;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Groovy 脚本执行器
 * <p>
 * 支持动态执行 Groovy 脚本，可访问 Spring 容器中的 Bean。
 * </p>
 */
public class ScriptExecutor {

    private static final Logger log = LoggerFactory.getLogger(ScriptExecutor.class);

    private final GroovyShell shell;
    private final ApplicationContext applicationContext;
    private final Map<String, Script> scriptCache = new ConcurrentHashMap<>();

    public ScriptExecutor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;

        // 配置编译器
        CompilerConfiguration config = new CompilerConfiguration();
        config.setSourceEncoding("UTF-8");

        // 创建 GroovyShell，使用当前类加载器
        this.shell = new GroovyShell(
                Thread.currentThread().getContextClassLoader(),
                createDefaultBinding(),
                config
        );
    }

    /**
     * 执行脚本
     *
     * @param scriptContent 脚本内容
     * @return 执行结果
     */
    public ScriptResult execute(String scriptContent) {
        return execute(scriptContent, null);
    }

    /**
     * 执行脚本（带参数）
     *
     * @param scriptContent 脚本内容
     * @param variables     变量绑定
     * @return 执行结果
     */
    public ScriptResult execute(String scriptContent, Map<String, Object> variables) {
        long startTime = System.currentTimeMillis();

        try {
            // 创建绑定
            Binding binding = createDefaultBinding();
            if (variables != null) {
                variables.forEach(binding::setVariable);
            }

            // 解析并执行脚本
            Script script = shell.parse(scriptContent);
            script.setBinding(binding);
            Object result = script.run();

            long costMs = System.currentTimeMillis() - startTime;
            log.info("Script executed successfully, cost: {}ms", costMs);

            return ScriptResult.success(result, costMs);

        } catch (Exception e) {
            long costMs = System.currentTimeMillis() - startTime;
            log.error("Script execution failed, cost: {}ms", costMs, e);
            return ScriptResult.failure(e.getMessage(), costMs);
        }
    }

    /**
     * 执行缓存的脚本
     *
     * @param scriptName 脚本名称
     * @param variables  变量绑定
     * @return 执行结果
     */
    public ScriptResult executeByName(String scriptName, Map<String, Object> variables) {
        Script script = scriptCache.get(scriptName);
        if (script == null) {
            return ScriptResult.failure("Script not found: " + scriptName, 0);
        }

        long startTime = System.currentTimeMillis();

        try {
            Binding binding = createDefaultBinding();
            if (variables != null) {
                variables.forEach(binding::setVariable);
            }
            script.setBinding(binding);
            Object result = script.run();

            long costMs = System.currentTimeMillis() - startTime;
            return ScriptResult.success(result, costMs);

        } catch (Exception e) {
            long costMs = System.currentTimeMillis() - startTime;
            log.error("Script execution failed: {}", scriptName, e);
            return ScriptResult.failure(e.getMessage(), costMs);
        }
    }

    /**
     * 注册脚本（预编译）
     *
     * @param scriptName    脚本名称
     * @param scriptContent 脚本内容
     */
    public void registerScript(String scriptName, String scriptContent) {
        Script script = shell.parse(scriptContent);
        scriptCache.put(scriptName, script);
        log.info("Script registered: {}", scriptName);
    }

    /**
     * 移除脚本
     */
    public void removeScript(String scriptName) {
        scriptCache.remove(scriptName);
        log.info("Script removed: {}", scriptName);
    }

    /**
     * 清空脚本缓存
     */
    public void clearCache() {
        scriptCache.clear();
        log.info("Script cache cleared");
    }

    /**
     * 创建默认绑定
     */
    private Binding createDefaultBinding() {
        Binding binding = new Binding();

        // 注入 Spring ApplicationContext
        binding.setVariable("ctx", applicationContext);

        // 注入常用工具
        binding.setVariable("log", log);

        // 提供获取 Bean 的快捷方法
        binding.setVariable("getBean", new BeanGetter(applicationContext));

        return binding;
    }

    /**
     * Bean 获取器（供脚本使用）
     */
    public static class BeanGetter {
        private final ApplicationContext ctx;

        public BeanGetter(ApplicationContext ctx) {
            this.ctx = ctx;
        }

        public Object call(String name) {
            return ctx.getBean(name);
        }

        public <T> T call(Class<T> clazz) {
            return ctx.getBean(clazz);
        }
    }

}

