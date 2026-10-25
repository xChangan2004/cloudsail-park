package com.changan.common.config.restclient;

import com.changan.common.config.wechat.WechatProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final WechatProperties wechatProperties;

    @Bean
    public RestClient wechatRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(wechatProperties.getConnectTimeout())) // 连接超时 5s
                .build();

        JdkClientHttpRequestFactory jdkFactory = new JdkClientHttpRequestFactory(httpClient);
        // 读取超时（从连接成功后，等待服务返回数据的超时）
        jdkFactory.setReadTimeout(Duration.ofSeconds(wechatProperties.getReadTimeout()));

        return RestClient.builder()
                .requestFactory(new BufferingClientHttpRequestFactory(jdkFactory))
                // 可以加默认拦截器、日志
                .build();
    }
}
