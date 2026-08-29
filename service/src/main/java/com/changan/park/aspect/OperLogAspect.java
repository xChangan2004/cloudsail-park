package com.changan.park.aspect;

import cn.hutool.json.JSONUtil;
import com.changan.common.utils.StpKit;
import com.changan.park.service.ISysOperateService;
import com.changan.common.config.operlog.OperLog;
import com.changan.common.enums.OperateStatus;
import com.changan.model.po.SysOperateLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    // 截断字符串的长度
    private static final int MAX_LENGTH = 2000;

    private final ISysOperateService sysOperateService;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint pjp, OperLog operLog) throws Throwable {
        // 1.记录开始时间
        long start = System.currentTimeMillis();
        Object result;
        SysOperateLog logEntity = new SysOperateLog();
        logEntity.setType(operLog.type());
        logEntity.setSubType(operLog.subType());
        try {
            // 2.执行原方法
            result = pjp.proceed();
            // 3.成功
            logEntity.setSuccess(OperateStatus.SUCCESS);
        } catch (Throwable e) {
            // 4.失败，记录异常后必须继续抛出，绝不吞业务异常
            logEntity.setSuccess(OperateStatus.FAIL);
            logEntity.setErrorMsg(truncate(e.toString()));
            throw e;
        } finally {
            // 5.无论成败，都补齐上下文并异步落库
            logEntity.setCostMs(System.currentTimeMillis() - start);
            logEntity.setUserId(getUserId());
            logEntity.setAction(buildAction(pjp, operLog));
            logEntity.setRequestMethod(getRequestMethod());
            logEntity.setRequestUrl(getRequestUrl());
            logEntity.setUserIp(getUserIp());
            saveQuietly(logEntity);
        }
        return result;
    }

    /**
     * 异步保存，任何异常只打日志，不影响主流程
     */
    private void saveQuietly(SysOperateLog logEntity) {
        try {
            sysOperateService.saveLog(logEntity);
        } catch (Exception e) {
            log.warn("操作日志保存失败 type={} subType={}", logEntity.getType(), logEntity.getSubType());
        }
    }

    /**
     * 组装参数JSON：敏感接口记ignored，序列化失败降级为toString，超长截断
     */
    private String buildAction(ProceedingJoinPoint pjp, OperLog operLog) {
        if (!operLog.recordParams()) {
            return "ignored";
        }
        try {
            return truncate(JSONUtil.toJsonStr(pjp.getArgs()));
        } catch (Exception e) {
            // 参数里可能有 MultipartFile 等无法序列化的对象
            return truncate(Arrays.toString(pjp.getArgs()));
        }
    }

    /**
     * 获取HttpServletRequest
     */
    private HttpServletRequest getRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            return null;
        }
        return ((ServletRequestAttributes) attributes).getRequest();
    }

    /**
     * 优先取 X-Forwarded-For 首段（内网穿透/代理场景），否则取直连地址
     */
    private String getUserIp() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String getRequestUrl() {
        HttpServletRequest request = getRequest();
        return request == null ? "" : request.getRequestURI();
    }

    private String getRequestMethod() {
        HttpServletRequest request = getRequest();
        return request == null ? "" : request.getMethod();
    }

    /**
     * 获取登录用户ID，未登录记0
     */
    private Long getUserId() {
        try {
            Object loginId = StpKit.ADMIN.getLoginId(0);
            return Long.parseLong(loginId.toString());
        } catch (Exception e) {
            return 0L;
        }
    }

    private String truncate(String str) {
        if (str == null) {
            return null;
        }
        return str.length() > MAX_LENGTH ? str.substring(0, MAX_LENGTH) : str;
    }
}
