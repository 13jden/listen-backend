package com.example.common.redis;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis 接入说明（Lettuce）：
 * <ul>
 *   <li><b>连接地址、密码、库号、连接池</b>：在应用的 {@code application*.properties} 里配置
 *       {@code spring.data.redis.host}、{@code port}、{@code password}、{@code database}、
 *       {@code spring.data.redis.lettuce.pool.*}，由 Spring Boot 自动创建 {@link RedisConnectionFactory}。</li>
 *   <li><b>{@code spring.data.redis.timeout}</b>：单条 Redis 命令在客户端侧的最大等待时间（毫秒）。
 *       与 {@code LPUSH}/{@code RPOP} 是否为「列表阻塞语义」无关：普通 {@code RPOP} 空列表会立即返回；
 *       出现 {@code RedisCommandTimeoutException} 多半是网络不可达、防火墙、Redis 未监听或实例过载。</li>
 *   <li>本类只负责：JSON 序列化的 {@link RedisTemplate}、以及（若使用发布订阅时）{@link RedisMessageListenerContainer}。</li>
 * </ul>
 */
@Configuration
public class RedisConfig<V> {
    @Bean("redisTemplate")
    public RedisTemplate<String, V> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, V> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.json());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setHashValueSerializer(RedisSerializer.json());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 显式 Lettuce 客户端选项（与 {@code spring.data.redis.timeout}、连接池仍配合使用）。
     * {@code pingBeforeActivateConnection}：从连接池取出连接时先 PING，减少 Redis 重启后用到半开连接的概率。
     */
    @Bean
    public LettuceClientConfigurationBuilderCustomizer lettuceClientConfigurationBuilderCustomizer() {
        return clientConfigurationBuilder -> clientConfigurationBuilder.clientOptions(
                ClientOptions.builder()
                        .protocolVersion(ProtocolVersion.RESP2)
                        .autoReconnect(true)
                        .pingBeforeActivateConnection(true)
                        .build());
    }

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}