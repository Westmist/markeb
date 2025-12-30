package org.markeb.hotswap.web;

import org.markeb.hotswap.HotSwapResult;
import org.markeb.hotswap.HotSwapService;
import org.markeb.hotswap.config.HotSwapProperties;
import org.markeb.hotswap.loader.OssClassBytesLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 热更新 HTTP 接口
 */
@RestController
@RequestMapping("${markeb.hotswap.http.path-prefix:/hotswap}")
public class HotSwapController {

    private static final Logger log = LoggerFactory.getLogger(HotSwapController.class);

    private final HotSwapService hotSwapService;
    private final HotSwapProperties properties;
    private final OssClassBytesLoader ossLoader;

    public HotSwapController(HotSwapService hotSwapService,
                             HotSwapProperties properties,
                             OssClassBytesLoader ossLoader) {
        this.hotSwapService = hotSwapService;
        this.properties = properties;
        this.ossLoader = ossLoader;
    }

    /**
     * 获取当前版本
     */
    @GetMapping("/version")
    public ResponseEntity<Map<String, Object>> getVersion() {
        Map<String, Object> result = new HashMap<>();
        result.put("version", hotSwapService.getCurrentVersion());
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }

    /**
     * 上传并热更新单个类
     *
     * @param className 全限定类名
     * @param file      .class 文件
     */
    @PostMapping("/upload")
    public ResponseEntity<HotSwapResult> uploadAndRedefine(
            @RequestParam("className") String className,
            @RequestParam("file") MultipartFile file) {

        log.info("Received hotswap request for class: {}", className);

        try {
            byte[] bytecode = file.getBytes();
            HotSwapResult result = hotSwapService.redefineClass(className, bytecode);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("Failed to read uploaded file", e);
            return ResponseEntity.badRequest()
                    .body(HotSwapResult.failure(className, "Failed to read file: " + e.getMessage()));
        }
    }

    /**
     * 从 OSS 加载并热更新单个类
     *
     * @param className 全限定类名
     */
    @PostMapping("/reload")
    public ResponseEntity<HotSwapResult> reloadFromOss(@RequestParam("className") String className) {
        log.info("Reloading class from OSS: {}", className);
        HotSwapResult result = hotSwapService.redefineClassFromOss(className);
        return ResponseEntity.ok(result);
    }

    /**
     * 应用热更包
     *
     * @param packagePath   热更包路径（OSS 上的路径）
     * @param targetVersion 目标版本号
     */
    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> applyPackage(
            @RequestParam("package") String packagePath,
            @RequestParam(value = "version", required = false) String targetVersion) {

        log.info("Applying hotswap package: {}, version: {}", packagePath, targetVersion);

        List<HotSwapResult> results = hotSwapService.applyPackage(packagePath, targetVersion);

        Map<String, Object> response = new HashMap<>();
        response.put("results", results);
        response.put("total", results.size());
        response.put("success", results.stream().filter(HotSwapResult::isSuccess).count());
        response.put("failed", results.stream().filter(r -> !r.isSuccess()).count());
        response.put("currentVersion", hotSwapService.getCurrentVersion());

        return ResponseEntity.ok(response);
    }

    /**
     * 列出可用的热更包
     */
    @GetMapping("/packages")
    public ResponseEntity<List<String>> listPackages() {
        List<String> packages = ossLoader.listPackages();
        return ResponseEntity.ok(packages);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("version", hotSwapService.getCurrentVersion());
        result.put("agentInitialized", org.markeb.hotswap.agent.HotSwapAgent.isInitialized());
        result.put("redefineSupported", org.markeb.hotswap.agent.HotSwapAgent.isRedefineClassesSupported());
        return ResponseEntity.ok(result);
    }

}

