package org.markeb.hotswap.loader;

import org.markeb.hotswap.config.HotSwapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * OSS 类字节码加载器
 * <p>
 * 从 OSS 加载热更包和类文件，支持多种 OSS 实现。
 * </p>
 */
public class OssClassBytesLoader implements ClassBytesLoader {

    private static final Logger log = LoggerFactory.getLogger(OssClassBytesLoader.class);

    private final OssClient ossClient;
    private final String pathPrefix;

    public OssClassBytesLoader(OssClient ossClient, HotSwapProperties.OssConfig config) {
        this.ossClient = ossClient;
        this.pathPrefix = config.getPathPrefix();
        log.info("OssClassBytesLoader initialized, type: {}, prefix: {}", ossClient.getType(), pathPrefix);
    }

    @Override
    public byte[] loadClass(String className) throws IOException {
        String objectKey = pathPrefix + className.replace('.', '/') + ".class";
        log.debug("Loading class from OSS: {}", objectKey);
        return ossClient.getObjectBytes(objectKey);
    }

    @Override
    public Map<String, byte[]> loadPackage(String packagePath) throws IOException {
        String objectKey = pathPrefix + packagePath;
        log.info("Loading hotswap package from OSS: {}", objectKey);

        Map<String, byte[]> classes = new HashMap<>();

        // 下载热更包到临时文件
        Path tempFile = Files.createTempFile("hotswap-", getFileExtension(packagePath));
        try {
            ossClient.downloadObject(objectKey, tempFile.toString());

            // 解压并读取 .class 文件
            if (packagePath.endsWith(".tar.gz") || packagePath.endsWith(".tgz")) {
                classes = extractTarGz(tempFile);
            } else if (packagePath.endsWith(".zip")) {
                classes = extractZip(tempFile);
            } else {
                throw new IOException("Unsupported package format: " + packagePath);
            }

            log.info("Loaded {} classes from package", classes.size());
            return classes;

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Override
    public boolean exists(String packagePath) {
        String objectKey = pathPrefix + packagePath;
        return ossClient.doesObjectExist(objectKey);
    }

    /**
     * 列出可用的热更包
     *
     * @return 热更包路径列表
     */
    public List<String> listPackages() {
        return ossClient.listObjects(pathPrefix).stream()
                .filter(key -> key.endsWith(".tar.gz") || key.endsWith(".zip"))
                .map(key -> key.substring(pathPrefix.length()))
                .toList();
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String path) {
        if (path.endsWith(".tar.gz")) return ".tar.gz";
        if (path.endsWith(".tgz")) return ".tgz";
        if (path.endsWith(".zip")) return ".zip";
        return "";
    }

    /**
     * 解压 tar.gz 文件
     */
    private Map<String, byte[]> extractTarGz(Path tarGzFile) throws IOException {
        Map<String, byte[]> classes = new HashMap<>();

        // 使用 ProcessBuilder 调用系统 tar 命令解压
        Path tempDir = Files.createTempDirectory("hotswap-extract-");
        try {
            ProcessBuilder pb = new ProcessBuilder("tar", "-xzf", tarGzFile.toString(), "-C", tempDir.toString());
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Failed to extract tar.gz, exit code: " + exitCode);
            }

            // 遍历解压后的目录，找出所有 .class 文件
            Files.walk(tempDir)
                    .filter(p -> p.toString().endsWith(".class"))
                    .forEach(p -> {
                        try {
                            String className = extractClassName(tempDir, p);
                            byte[] bytecode = Files.readAllBytes(p);
                            classes.put(className, bytecode);
                        } catch (IOException e) {
                            log.error("Failed to read class file: {}", p, e);
                        }
                    });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while extracting tar.gz", e);
        } finally {
            deleteDirectory(tempDir);
        }

        return classes;
    }

    /**
     * 解压 zip 文件
     */
    private Map<String, byte[]> extractZip(Path zipFile) throws IOException {
        Map<String, byte[]> classes = new HashMap<>();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    String className = entry.getName()
                            .replace('/', '.')
                            .replaceAll("\\.class$", "");

                    // 移除可能的前缀目录（如 classes/）
                    if (className.startsWith("classes.")) {
                        className = className.substring("classes.".length());
                    }

                    byte[] bytecode = zis.readAllBytes();
                    classes.put(className, bytecode);
                }
                zis.closeEntry();
            }
        }

        return classes;
    }

    /**
     * 从文件路径提取类名
     */
    private String extractClassName(Path baseDir, Path classFile) {
        String relativePath = baseDir.relativize(classFile).toString();

        // 移除可能的前缀目录
        if (relativePath.contains("classes" + File.separator)) {
            int idx = relativePath.indexOf("classes" + File.separator);
            relativePath = relativePath.substring(idx + "classes".length() + 1);
        }

        return relativePath
                .replace(File.separatorChar, '.')
                .replaceAll("\\.class$", "");
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(Path dir) {
        try {
            Files.walk(dir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            log.warn("Failed to delete: {}", p);
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to delete directory: {}", dir);
        }
    }

}
