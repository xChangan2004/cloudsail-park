package com.changan.common.exceptions;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.changan.common.domain.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public R<Void> handleNotLoginException(NotLoginException e, HttpServletRequest request) {
        log.warn("请求[{} {}] 未登录: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return R.error(401, "未登录，请先登录");
    }

    @ExceptionHandler(NotRoleException.class)
    public R<Void> handleNotRoleException(NotRoleException e, HttpServletRequest request) {
        log.warn("请求[{} {}] 角色不足: 需要[{}]", request.getMethod(), request.getRequestURI(), e.getRole());
        return R.error(403, "权限不足，无法访问");
    }

    @ExceptionHandler(NotPermissionException.class)
    public R<Void> handleNotPermissionException(NotPermissionException e, HttpServletRequest request) {
        log.warn("请求[{} {}] 权限不足: 需要[{}]", request.getMethod(), request.getRequestURI(), e.getPermission());
        return R.error(403, "权限不足，无法访问");
    }

    @ExceptionHandler(UnauthorizedException.class)
    public R<Void> handleUnauthorizedException(UnauthorizedException e, HttpServletRequest request) {
        log.warn("请求[{} {}] 未授权: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return R.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public R<Void> handleForbiddenException(ForbiddenException e, HttpServletRequest request) {
        log.warn("请求[{} {}] 无权限: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return R.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public R<Void> handleBadRequestException(BadRequestException e, HttpServletRequest request) {
        log.warn("请求[{} {}] 请求错误: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return R.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(BizIllegalException.class)
    public R<Void> handleBizIllegalException(BizIllegalException e, HttpServletRequest request) {
        log.warn("请求[{} {}] 业务异常: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return R.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(DbException.class)
    public R<Void> handleDbException(DbException e, HttpServletRequest request) {
        log.error("请求[{} {}] 数据库异常: ", request.getMethod(), request.getRequestURI(), e);
        return R.error("系统繁忙，请稍后重试");
    }

    @ExceptionHandler(RequestTimeoutException.class)
    public R<Void> handleRequestTimeoutException(RequestTimeoutException e, HttpServletRequest request) {
        log.warn("请求[{} {}] 请求超时: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return R.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidException(MethodArgumentNotValidException e) {
        String message = Objects.requireNonNull(e.getBindingResult().getFieldError()).getDefaultMessage();
        log.warn("参数校验异常: {}", message);
        return R.error(message);
    }

    @ExceptionHandler(BindException.class)
    public R<Void> handleBindException(BindException e) {
        String message = Objects.requireNonNull(e.getBindingResult().getFieldError()).getDefaultMessage();
        log.warn("参数绑定异常: {}", message);
        return R.error(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验异常: {}", message);
        return R.error(message);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public R<Void> handleParamException(Exception e) {
        log.warn("参数异常: {}", e.getMessage());
        return R.error("请求参数不合法");
    }

    @ExceptionHandler(CommonException.class)
    public R<Void> handleCommonException(CommonException e, HttpServletRequest request) {
        log.warn("请求[{} {}] 通用异常: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return R.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public R<Void> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("请求[{} {}] 运行时异常: ", request.getMethod(), request.getRequestURI(), e);
        return R.error("系统繁忙，请稍后重试");
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("请求[{} {}] 系统异常: ", request.getMethod(), request.getRequestURI(), e);
        return R.error("系统繁忙，请稍后重试");
    }
}
