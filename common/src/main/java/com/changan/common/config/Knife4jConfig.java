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
                        .description("CloudSail Park 云帆智泊管理端接口文档")
                        .version("v1")
                        .contact(new Contact()
                                .name("长安")
                                .email("17725716661@163.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://github.com/xChangan2004/CloudSail-Park-Admin")));
    }

    @Bean
    public GroupedOpenApi systemAPI() {
        return GroupedOpenApi.builder()
                .group("系统管理")
                .pathsToMatch("/system/**")
                .build();
    }

    @Bean
    public GroupedOpenApi parkingLotAPI() {
        return GroupedOpenApi.builder()
                .group("停车场管理")
                .pathsToMatch("/parking/**")
                .build();
    }

    @Bean
    public GroupedOpenApi customerAPI() {
        return GroupedOpenApi.builder()
                .group("客户管理")
                .pathsToMatch("/customer/**")
                .build();
    }

    @Bean
    public GroupedOpenApi entryExitAPI() {
        return GroupedOpenApi.builder()
                .group("出入记录管理")
                .pathsToMatch("/record/**")
                .build();
    }

    @Bean
    public GroupedOpenApi paymentAPI() {
        return GroupedOpenApi.builder()
                .group("支付管理")
                .pathsToMatch("/pay/**", "/payment/**", "/refund/**")
                .build();
    }

    @Bean
    public GroupedOpenApi dashboardAPI() {
        return GroupedOpenApi.builder()
                .group("数据看板管理")
                .pathsToMatch("/dashboard/**")
                .build();
    }
}
