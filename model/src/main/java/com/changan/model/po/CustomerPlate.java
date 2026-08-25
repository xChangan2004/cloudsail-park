package com.changan.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.changan.common.constants.RegexConstants;
import com.changan.common.enums.EnergyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("customer_plate")
@Schema(description = "客户车牌实体")
public class CustomerPlate extends BaseEntity {

    @NotNull(message = "客户ID不能为空")
    @Schema(description = "客户ID")
    private Long customerId;

    @NotBlank(message = "车牌号码不能为空")
    @Pattern(regexp = RegexConstants.PLATE_NUMBER, message = "车牌号格式不正确")
    @Schema(description = "车牌号码")
    private String plateNumber;

    @Schema(description = "能源类型：1燃油车，2新能源")
    @NotNull(message = "能源类型不能为空")
    private EnergyType energyType;

    @Schema(description = "是否默认车牌")
    private Boolean isDefault = false;
}
