package com.changan.common.config.mvc;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import com.changan.common.converter.StringToBaseEnumConverterFactory;
import com.changan.common.utils.StpKit;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new StringToBaseEnumConverterFactory());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 开启注解鉴权（@SaCheckLogin、@SaCheckRole...）
        registry.addInterceptor(new SaInterceptor(handler -> {
            // 用户端：校验app账号空间登录
            SaRouter.match("/app/**")
                    .notMatch("/app/login")
                    .check(r -> StpKit.APP.checkLogin());

            // 管理端：除放行名单外，强制校验admin登录（注解校验不受影响，仍并行生效）
            SaRouter.match("/admin/**")
                    .notMatch("/admin/system/user/login")
                    .notMatch("/admin/pay/notify")
                    .check(r -> StpKit.ADMIN.checkLogin());
        })).addPathPatterns("/**");
    }

    /**
     * 配置跨域
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
