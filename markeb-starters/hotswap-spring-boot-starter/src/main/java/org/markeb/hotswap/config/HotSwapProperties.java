package org.markeb.hotswap.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 热更新配置属性
 */
@Data
@ConfigurationProperties(prefix = "markeb.hotswap")
public class HotSwapProperties {

    /**
     * 是否启用热更新
     */
    private boolean enabled = true;

    /**
     * 当前运行版本
     */
    private String version = "unknown";

    /**
     * 版本文件路径（用于持久化版本号）
     */
    private String versionFile = "./hotswap-version.txt";

    /**
     * OSS 配置
     */
    private OssConfig oss = new OssConfig();

    /**
     * HTTP 接口配置
     */
    private HttpConfig http = new HttpConfig();

    /**
     * Groovy 脚本配置
     */
    private ScriptConfig script = new ScriptConfig();

    @Data
    public static class OssConfig {

        /**
         * OSS 类型：aliyun, tencent, minio, aws-s3
         */
        private String type = "aliyun";

        /**
         * OSS Endpoint
         */
        private String endpoint;

        /**
         * 区域（腾讯云、AWS 需要）
         */
        private String region;

        /**
         * Access Key ID
         */
        private String accessKeyId;

        /**
         * Access Key Secret
         */
        private String accessKeySecret;

        /**
         * Bucket 名称
         */
        private String bucketName;

        /**
         * 热更包存放路径前缀
         */
        private String pathPrefix = "hotswap/";

    }

    @Data
    public static class HttpConfig {

        /**
         * 是否启用 HTTP 接口
         */
        private boolean enabled = true;

        /**
         * 接口路径前缀
         */
        private String pathPrefix = "/hotswap";

        /**
         * 是否需要认证
         */
        private boolean authEnabled = false;

        /**
         * 认证 Token（简单认证）
         */
        private String authToken;

    }

    @Data
    public static class ScriptConfig {

        /**
         * 是否启用 Groovy 脚本执行
         */
        private boolean enabled = true;

    }

}

