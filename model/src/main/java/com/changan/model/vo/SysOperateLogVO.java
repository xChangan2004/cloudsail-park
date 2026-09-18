package com.changan.model.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.changan.common.converter.OperateStatusConverter;
import com.changan.common.enums.OperateStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "操作日志分页VO")
public class SysOperateLogVO {

    @Schema(description = "日志ID")
    @ExcelProperty(value = "操作序号")
    private Long id;

    @Schema(description = "操作人用户名")
    @ExcelProperty(value = "操作人员")
    private String username;

    @Schema(description = "操作模块")
    @ExcelProperty(value = "操作模块")
    private String type;

    @Schema(description = "操作名")
    @ExcelProperty(value = "操作名")
    private String subType;

    @Schema(description = "请求参数JSON")
    @ExcelProperty(value = "请求参数")
    private String action;

    @Schema(description = "操作结果")
    @ExcelProperty(value = "操作结果", converter = OperateStatusConverter.class)
    private OperateStatus success;

    @Schema(description = "失败原因")
    @ExcelProperty(value = "失败原因")
    private String errorMsg;

    @Schema(description = "耗时（毫秒）")
    @ExcelProperty(value = "耗时（毫秒）")
    private Long costMs;

    @Schema(description = "请求方法")
    @ExcelProperty(value = "请求方法")
    private String requestMethod;

    @Schema(description = "请求地址")
    @ExcelProperty(value = "请求地址")
    private String requestUrl;

    @Schema(description = "用户IP")
    @ExcelProperty(value = "请求IP")
    private String userIp;

    @Schema(description = "操作时间")
    @ExcelProperty(value = "请求时间")
    private LocalDateTime createTime;

    @Schema(description = "操作方法")
    @ExcelProperty(value = "操作方法")
    private String method;

    @Schema(description = "操作结果")
    @ExcelProperty(value = "操作结果")
    private String jsonResult;
}