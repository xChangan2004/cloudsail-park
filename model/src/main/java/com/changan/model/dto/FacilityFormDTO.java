package com.changan.model.dto;

import com.changan.common.enums.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "服务设施表单DTO")
public class FacilityFormDTO {

    @Schema(description = "设施ID")
    private Long id;

    @NotBlank(message = "设施编码不能为空")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,31}$",
            message = "编码需大写字母开头，只含大写字母/数字/下划线，2~32位")
    @Schema(description = "编码，新增后不可修改")
    private String code;

    @NotBlank(message = "设施名称不能为空")
    @Size(max = 32, message = "名称最多 32 个字")
    @Schema(description = "显示名称")
    private String name;

    @Size(max = 32, message = "图标标识最多 32 个字符")
    @Schema(description = "图标标识")
    private String icon;

    @Schema(description = "排序，越小越靠前")
    private Integer sort;

    @Schema(description = "状态")
    private CommonStatus status;
}
