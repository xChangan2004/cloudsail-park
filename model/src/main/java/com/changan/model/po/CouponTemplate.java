package com.changan.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.changan.common.enums.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("coupon_template")
@Schema(description = "优惠券模板实体类")
public class CouponTemplate extends BaseEntity {

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

    @Schema(description = "每人限领数")
    private Integer perLimit;

    @Schema(description = "有效天数")
    private Integer validDays;

    @Schema(description = "状态")
    private CommonStatus status;
}
