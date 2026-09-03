package com.changan.common.config.mvc;

import cn.dev33.satoken.SaManager;
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

    static {
        // 启动时立即注册自定义 StpLogic，避免 StpKit 懒加载导致注解校验（@SaCheckPermission 按 type 查注册表）找不到实例
        SaManager.putStpLogic(StpKit.ADMIN);
        SaManager.putStpLogic(StpKit.APP);
    }

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
                    .notMatch("/app/auth/login")
                    .notMatch("/app/auth/code")
                    .check(r -> StpKit.APP.checkLogin());

            // 管理端：除放行名单外，强制校验admin登录（注解校验不受影响，仍并行生效）
            SaRouter.match("/admin/**")
                    .notMatch("/admin/system/user/login")
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
