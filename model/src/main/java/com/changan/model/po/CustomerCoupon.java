package com.changan.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.changan.common.enums.CustomerCouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("customer_coupon")
@Schema(description = "客户优惠券实体")
public class CustomerCoupon extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "持有人ID")
    private Long customerId;

    @Schema(description = "关联订单ID")
    private Long orderId;

    @Schema(description = "状态")
    private CustomerCouponStatus status;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;
}
