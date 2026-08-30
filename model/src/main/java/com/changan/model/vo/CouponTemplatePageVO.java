package com.changan.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "优惠券模板分页VO")
public class CouponTemplatePageVO {

    @Schema(description = "模板ID")
    private Long id;

    @Schema(description = "券名称")
    private String name;

    @Schema(description = "使用门槛")
    private BigDecimal threshold;

    @Schema(description = "抵扣金额")
    private BigDecimal amount;

    @Schema(description = "发放总量")
    private Integer totalCount;

    @Schema(description = "已发放数")
    private Integer issuedCount;

    @Schema(description = "领取进度百分比")
    private Integer receivedRate;

    @Schema(description = "每人限领")
    private Integer perLimit;

    @Schema(description = "有效天数")
    private Integer validDays;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}