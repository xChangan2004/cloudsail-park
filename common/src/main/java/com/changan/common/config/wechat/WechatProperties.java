package com.changan.common.config.wechat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wechat")
public class WechatProperties {

    /**
     * 小程序ID
     */
    private String appid;

    /**
     * 小程序秘钥
     */
    private String appSecret;

    /**
     * 登录凭证校验URL
     */
    private String jscode2sessionUrl;

    /**
     * 获取接口调用凭据URL
     */
    private String accessTokenUrl;

    /**
     * 获取手机号URL
     */
    private String phoneNumberUrl;

    /**
     * 连接超时时间
     */
    private int connectTimeout;

    /**
     * 读取内容超时时间
     */
    private int readTimeout;
}
