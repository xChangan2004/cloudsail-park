package com.changan.common.config.redisson;

import com.changan.common.config.redisson.aspect.LockAspect;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Slf4j
@AutoConfiguration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private Integer port;

    @Value("${spring.data.redis.password}")
    private String password;

    @Value("${spring.data.redis.database}")
    private Integer database;

    @Value("${spring.data.redis.timeout}")
    private Integer timeout;

    @Bean
    public RedissonClient redissonClient() {
        log.debug("尝试初始化RedissonClient");
        // 1.设置Redisson配置
        Config config = new Config();
        // 单机模式
        config.useSingleServer()
                .setAddress(String.format("redis://%s:%d", host, port))
                .setConnectTimeout(timeout)
                .setDatabase(database)
                .setPassword(password)
                .setConnectionPoolSize(16)
                .setConnectionMinimumIdleSize(4)
                .setIdleConnectionTimeout(10000);
        // 2.创建Redisson客户端
        return Redisson.create(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedissonClient redissonClient) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 用 Redisson 连接
        template.setConnectionFactory(new RedissonConnectionFactory(redissonClient));

        // Key 序列化
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value 序列化
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public LockAspect lockAspect(RedissonClient redissonClient) {
        return new LockAspect(redissonClient);
    }
}