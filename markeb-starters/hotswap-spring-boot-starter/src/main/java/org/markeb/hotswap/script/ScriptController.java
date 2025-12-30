package org.markeb.hotswap.script;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Groovy 脚本执行 HTTP 接口
 */
@RestController
@RequestMapping("${markeb.hotswap.http.path-prefix:/hotswap}/script")
public class ScriptController {

    private static final Logger log = LoggerFactory.getLogger(ScriptController.class);

    private final ScriptExecutor scriptExecutor;

    public ScriptController(ScriptExecutor scriptExecutor) {
        this.scriptExecutor = scriptExecutor;
    }

    /**
     * 执行 Groovy 脚本
     *
     * @param request 请求体
     */
    @PostMapping("/execute")
    public ResponseEntity<ScriptResult> execute(@RequestBody ScriptRequest request) {
        log.info("Executing script, length: {}", request.getScript().length());

        ScriptResult result = scriptExecutor.execute(request.getScript(), request.getVariables());
        return ResponseEntity.ok(result);
    }

    /**
     * 注册脚本（预编译）
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody ScriptRegisterRequest request) {
        log.info("Registering script: {}", request.getName());

        scriptExecutor.registerScript(request.getName(), request.getScript());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("name", request.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * 执行已注册的脚本
     */
    @PostMapping("/run/{name}")
    public ResponseEntity<ScriptResult> runByName(
            @PathVariable String name,
            @RequestBody(required = false) Map<String, Object> variables) {

        log.info("Running registered script: {}", name);

        ScriptResult result = scriptExecutor.executeByName(name, variables);
        return ResponseEntity.ok(result);
    }

    /**
     * 移除已注册的脚本
     */
    @DeleteMapping("/remove/{name}")
    public ResponseEntity<Map<String, Object>> remove(@PathVariable String name) {
        log.info("Removing script: {}", name);

        scriptExecutor.removeScript(name);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("name", name);
        return ResponseEntity.ok(response);
    }

    /**
     * 清空脚本缓存
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clear() {
        log.info("Clearing script cache");

        scriptExecutor.clearCache();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    /**
     * 脚本执行请求
     */
    @lombok.Data
    public static class ScriptRequest {
        /**
         * 脚本内容
         */
        private String script;

        /**
         * 变量绑定
         */
        private Map<String, Object> variables;
    }

    /**
     * 脚本注册请求
     */
    @lombok.Data
    public static class ScriptRegisterRequest {
        /**
         * 脚本名称
         */
        private String name;

        /**
         * 脚本内容
         */
        private String script;
    }

}

