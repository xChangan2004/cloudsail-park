package com.changan.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.changan.common.enums.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("parking_lot")
@Schema(name = "停车场实体")
public class ParkingLot extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "停车场名称")
    private String name;

    @Schema(description = "停车场地址")
    private String address;

    @Schema(description = "总车位数")
    private Integer totalSpaces;

    @Schema(description = "可用车位数")
    private Integer availableSpaces;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "联系方式")
    private String contact;

    @Schema(description = "状态")
    private CommonStatus status;
}
