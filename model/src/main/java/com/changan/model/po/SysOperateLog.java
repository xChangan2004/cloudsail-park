package com.changan.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.changan.common.enums.OperateStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_operate_log")
@Schema(description = "系统操作记录")
public class SysOperateLog extends BaseEntity {

    @Schema(description = "操作用户ID")
    private Long userId;

    @Schema(description = "操作模块")
    private String type;

    @Schema(description = "操作名")
    private String subType;

    @Schema(description = "请求参数JSON")
    private String action;

    @Schema(description = "操作结果")
    private OperateStatus success;

    @Schema(description = "失败时的异常信息")
    private String errorMsg;

    @Schema(description = "耗时（毫秒）")
    private Long costMs;

    @Schema(description = "请求方法")
    private String requestMethod;

    @Schema(description = "请求地址")
    private String requestUrl;

    @Schema(description = "用户IP")
    private String userIp;
}
