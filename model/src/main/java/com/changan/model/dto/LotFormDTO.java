package com.changan.model.dto;

import com.changan.common.enums.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class LotFormDTO {

    @Schema(description = "停车场ID")
    private Long id;

    @NotBlank(message = "停车场名称不能为空")
    @Schema(description = "停车场名称")
    private String name;

    @Schema(description = "停车场地址")
    private String address;

    @NotNull(message = "总车位数不能为空")
    @Schema(description = "总车位数")
    private Integer totalSpaces;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "联系方式")
    @NotBlank(message = "联系方式不能为空")
    private String contact;

    @Schema(description = "封面图（相对路径）")
    private String cover;

    @Size(max = 9, message = "轮播图最多 9 张")
    @Schema(description = "轮播图（相对路径数组）")
    private List<String> images;

    @Size(max = 10, message = "服务设施最多选 10 个")
    @Schema(description = "服务设施编码数组")
    private List<String> facilityCodes;

    @Schema(description = "状态")
    private CommonStatus status;
}
