package com.changan.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "用户端停车场信息")
public class AppParkingLotVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "停车场ID")
    private Long id;

    @Schema(description = "停车场名称")
    private String name;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "联系方式")
    private String contact;

    @Schema(description = "总车位数")
    private Integer totalSpaces;

    @Schema(description = "当前空闲车位数")
    private Long freeSpaces;

    @Schema(description = "是否有空位")
    private Boolean hasFree;

    @Schema(description = "距离（米），未传定位或该场无坐标时为null")
    private Long distance;
}