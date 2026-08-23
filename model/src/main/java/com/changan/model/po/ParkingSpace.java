package com.changan.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.changan.common.enums.ParkingSpaceStatus;
import com.changan.common.enums.ParkingSpaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("parking_space")
@Schema(name = "停车场车位实体")
public class ParkingSpace extends BaseEntity implements Serializable {

    @NotNull(message = "所属停车场ID不能为空")
    @Schema(description = "所属停车场ID")
    private Long lotId;

    @NotBlank(message = "车位编号不能为空")
    @Schema(description = "车位编号（如A-001）")
    private String spaceCode;

    @NotNull(message = "车位类型不能为空")
    @Schema(description = "车位类型")
    private ParkingSpaceType spaceType;

    @NotNull(message = "车位状态不能为空")
    @Schema(description = "状态")
    private ParkingSpaceStatus status;
}
