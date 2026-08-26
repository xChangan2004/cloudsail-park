package com.changan.common.config.alipay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class AlipayProperties {

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 应用私钥
     */
    private String merchantPrivateKey;

    /**
     * 支付宝公钥
     */
    private String alipayPublicKey;

    /**
     * 异步通知地址
     */
    private String notifyUrl;

    /**
     * 同步通知地址
     */
    private String returnUrl;

    /**
     * 签名类型
     */
    private String signType;

    /**
     * 编码
     */
    private String charset;

    /**
     * 网关地址
     */
    private String gatewayUrl;
}
