package com.changan.model.dto;

import com.changan.common.enums.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ParkingLotFormDTO {

    @Schema(description = "停车场ID")
    private Long id;

    @NotBlank(message = "停车场名称不能为空")
    @Schema(description = "停车场名称")
    private String name;

    @Schema(description = "停车场地址")
    private String address;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "联系方式")
    @NotBlank(message = "联系方式不能为空")
    private String contact;

    @Schema(description = "状态")
    private CommonStatus status;
}
