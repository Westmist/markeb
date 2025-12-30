package org.markeb.net.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.markeb.net.config.NetworkProperties;
import org.markeb.net.serialization.CodecType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 消息适配器自动配置
 * <p>
 * 配置协议无关的消息解析和处理组件。
 * </p>
 */
@AutoConfiguration
@EnableConfigurationProperties(NetworkProperties.class)
@ConditionalOnProperty(prefix = "markeb.network", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MessageAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MessageAutoConfiguration.class);

    /**
     * 消息解析器
     * <p>
     * 根据配置的编解码类型创建对应的解析器。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    public IMessageParser messageParser(NetworkProperties properties, ObjectMapper objectMapper) {
        CodecType codecType = properties.getCodec();
        log.info("Creating IMessageParser with codec type: {}", codecType);

        return switch (codecType) {
            case PROTOBUF -> new ProtobufMessageParser();
            case JSON -> new JsonMessageParser(objectMapper);
            case PROTOSTUFF -> new ProtostuffMessageParser();
        };
    }

    /**
     * 统一消息分发器
     */
    @Bean
    @ConditionalOnMissingBean
    public UnifiedMessageDispatcher unifiedMessageDispatcher(IMessageParser messageParser) {
        log.info("Creating UnifiedMessageDispatcher with parser: {}", messageParser.getCodecType());
        return new UnifiedMessageDispatcher(messageParser);
    }

    /**
     * 消息适配器工厂
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageAdapterFactory messageAdapterFactory(NetworkProperties properties, IMessageParser messageParser, ObjectMapper objectMapper) {
        CodecType codecType = properties.getCodec();
        log.info("Creating MessageAdapterFactory with codec type: {}", codecType);

        MessageAdapterFactory factory = new MessageAdapterFactory(codecType, clazz -> messageParser.getMessageId(clazz));
        if (codecType == CodecType.JSON) {
            factory.withObjectMapper(objectMapper);
        }
        return factory;
    }
}

