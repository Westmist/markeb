package org.markeb.hotswap.config;

import org.markeb.hotswap.HotSwapService;
import org.markeb.hotswap.loader.*;
import org.markeb.hotswap.script.ScriptController;
import org.markeb.hotswap.script.ScriptExecutor;
import org.markeb.hotswap.web.HotSwapAuthInterceptor;
import org.markeb.hotswap.web.HotSwapController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 热更新自动配置
 */
@Configuration
@EnableConfigurationProperties(HotSwapProperties.class)
@ConditionalOnProperty(prefix = "markeb.hotswap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HotSwapAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(HotSwapAutoConfiguration.class);

    /**
     * 阿里云 OSS 客户端
     */
    @Bean
    @ConditionalOnMissingBean(OssClient.class)
    @ConditionalOnClass(name = "com.aliyun.oss.OSS")
    @ConditionalOnProperty(prefix = "markeb.hotswap.oss", name = "type", havingValue = "aliyun", matchIfMissing = true)
    public OssClient aliyunOssClient(HotSwapProperties properties) {
        log.info("Initializing Aliyun OSS client");
        return new AliyunOssClient(properties.getOss());
    }

    /**
     * 腾讯云 COS 客户端
     */
    @Bean
    @ConditionalOnMissingBean(OssClient.class)
    @ConditionalOnClass(name = "com.qcloud.cos.COSClient")
    @ConditionalOnProperty(prefix = "markeb.hotswap.oss", name = "type", havingValue = "tencent")
    public OssClient tencentCosClient(HotSwapProperties properties) {
        log.info("Initializing Tencent COS client");
        return new TencentCosClient(properties.getOss());
    }

    /**
     * Google Cloud Storage 客户端
     */
    @Bean
    @ConditionalOnMissingBean(OssClient.class)
    @ConditionalOnClass(name = "com.google.cloud.storage.Storage")
    @ConditionalOnProperty(prefix = "markeb.hotswap.oss", name = "type", havingValue = "google")
    public OssClient googleGcsClient(HotSwapProperties properties) {
        log.info("Initializing Google GCS client");
        return new GoogleGcsClient(properties.getOss());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "markeb.hotswap.oss", name = "endpoint")
    public OssClassBytesLoader ossClassBytesLoader(OssClient ossClient, HotSwapProperties properties) {
        log.info("Initializing OSS class bytes loader, type: {}", ossClient.getType());
        return new OssClassBytesLoader(ossClient, properties.getOss());
    }

    @Bean
    @ConditionalOnMissingBean
    public HotSwapService hotSwapService(HotSwapProperties properties,
                                         ClassBytesLoader classLoader,
                                         ApplicationEventPublisher eventPublisher) {
        log.info("Initializing HotSwap service, version: {}", properties.getVersion());
        return new HotSwapService(properties, classLoader, eventPublisher);
    }

    @Bean
    @ConditionalOnProperty(prefix = "markeb.hotswap.http", name = "enabled", havingValue = "true", matchIfMissing = true)
    public HotSwapController hotSwapController(HotSwapService hotSwapService,
                                               HotSwapProperties properties,
                                               OssClassBytesLoader ossLoader) {
        log.info("Initializing HotSwap controller");
        return new HotSwapController(hotSwapService, properties, ossLoader);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "groovy.lang.GroovyShell")
    @ConditionalOnProperty(prefix = "markeb.hotswap.script", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ScriptExecutor scriptExecutor(ApplicationContext applicationContext) {
        log.info("Initializing Groovy script executor");
        return new ScriptExecutor(applicationContext);
    }

    @Bean
    @ConditionalOnClass(name = "groovy.lang.GroovyShell")
    @ConditionalOnProperty(prefix = "markeb.hotswap.script", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ScriptController scriptController(ScriptExecutor scriptExecutor) {
        log.info("Initializing Script controller");
        return new ScriptController(scriptExecutor);
    }

    @Configuration
    @ConditionalOnProperty(prefix = "markeb.hotswap.http", name = "auth-enabled", havingValue = "true")
    static class HotSwapWebConfig implements WebMvcConfigurer {

        private final HotSwapProperties properties;

        HotSwapWebConfig(HotSwapProperties properties) {
            this.properties = properties;
        }

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            String pathPrefix = properties.getHttp().getPathPrefix();
            registry.addInterceptor(new HotSwapAuthInterceptor(properties))
                    .addPathPatterns(pathPrefix + "/**");
        }
    }

}
