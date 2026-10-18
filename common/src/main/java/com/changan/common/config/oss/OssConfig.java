package com.changan.common.config.oss;

import cn.hutool.core.util.StrUtil;
import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.OSSClientBuilder;
import com.aliyun.sdk.service.oss2.credentials.StaticCredentialsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class OssConfig {

    /**
     * 配置OSS Client
     *
     */
    @Bean(destroyMethod = "close")
    public OSSClient ossClient(OssProperties properties) {
        log.info("OSS配置AK{}, {}", properties.getAccessKeyId(), properties.getAccessKeySecret());
        // 启动前置校验，缺失AK/region直接启动失败，早发现问题
        if (StrUtil.isBlank(properties.getAccessKeyId()) || StrUtil.isBlank(properties.getAccessKeySecret())) {
            throw new IllegalArgumentException("OSS AK配置缺失，请检查oss配置");
        }
        if (StrUtil.isBlank(properties.getRegion())) {
            throw new IllegalArgumentException("OSS region不能为空");
        }
        OSSClientBuilder builder = OSSClient.newBuilder()
                .credentialsProvider(new StaticCredentialsProvider(
                        properties.getAccessKeyId(),
                        properties.getAccessKeySecret()))
                .region(properties.getRegion());
        if (StrUtil.isNotBlank(properties.getEndpoint())) {
            builder.endpoint(properties.getEndpoint());
        }
        log.info("初始化 OSSClient: region={}, bucket={}", properties.getRegion(), properties.getBucket());
        return builder.build();
    }
}
