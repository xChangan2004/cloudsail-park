package com.changan.common.config.oss;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssProperties {

    /**
     * 访问域名，如 oss-cn-hangzhou.aliyuncs.com
     */
    private String endpoint;

    /**
     * 地域，如 cn-hangzhou
     */
    private String region;

    /**
     * Bucket
     */
    private String bucket;

    /**
     * AccessKeyId
     */
    private String accessKeyId;

    /**
     * AccessKeySecret
     */
    private String accessKeySecret;

    /**
     * 自定义访问域名（CDN 或绑定域名），为空则用默认外网域名
     */
    private String publicDomain;

    /**
     * 单文件大小上限（字节）
     */
    private Long maxSize = 10 * 1024 * 1024L;

    /**
     * 允许的扩展名
     */
    private Set<String> allowExt = Set.of("jpg", "jpeg", "png", "webp", "gif");
}
