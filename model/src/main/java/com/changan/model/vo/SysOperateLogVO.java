package com.changan.model.vo;

import com.changan.common.enums.OperateStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "操作日志分页VO")
public class SysOperateLogVO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "操作人用户名")
    private String username;

    @Schema(description = "操作模块")
    private String type;

    @Schema(description = "操作名")
    private String subType;

    @Schema(description = "请求参数JSON")
    private String action;

    @Schema(description = "操作结果")
    private OperateStatus success;

    @Schema(description = "失败原因")
    private String errorMsg;

    @Schema(description = "耗时（毫秒）")
    private Long costMs;

    @Schema(description = "请求方法")
    private String requestMethod;

    @Schema(description = "请求地址")
    private String requestUrl;

    @Schema(description = "用户IP")
    private String userIp;

    @Schema(description = "操作时间")
    private LocalDateTime createTime;
}