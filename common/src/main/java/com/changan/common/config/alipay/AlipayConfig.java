package com.changan.common.config.alipay;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AlipayConfig {

    private final AlipayProperties properties;

    @Bean
    public AlipayClient alipayClient() throws AlipayApiException {
        return new DefaultAlipayClient(
                properties.getGatewayUrl(),
                properties.getAppId(),
                properties.getMerchantPrivateKey(),
                "json",
                properties.getCharset(),
                properties.getAlipayPublicKey(),
                properties.getSignType()
        );
    }
}
