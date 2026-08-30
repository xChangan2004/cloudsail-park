package com.changan.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class Knife4jConfig {

    @Bean
    public OpenAPI defaultOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CloudSail Park ADMIN API文档")
                        .description("CloudSail Park 云帆智泊接口文档")
                        .version("v1")
                        .contact(new Contact()
                                .name("长安")
                                .email("17725716661@163.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://github.com/xChangan2004/CloudSail-Park-Admin")));
    }

    /** 管理端接口：/admin/** */
    @Bean
    public GroupedOpenApi adminAPI() {
        return GroupedOpenApi.builder()
                .group("管理端")
                .pathsToMatch("/admin/**")
                .build();
    }

    /** 用户端接口：/app/** */
    @Bean
    public GroupedOpenApi appAPI() {
        return GroupedOpenApi.builder()
                .group("用户端")
                .pathsToMatch("/app/**")
                .build();
    }

    @Bean
    public GroupedOpenApi payNotifyAPI() {
        return GroupedOpenApi.builder()
                .group("支付回调")
                .pathsToMatch("/notify/**")
                .build();
    }
}
