package com.changan.model.dto;

import com.changan.common.constants.RegexConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EntryFormDTO {

    @NotNull(message = "停车场ID不能为空")
    @Schema(description = "停车场ID")
    private Long lotId;

    @NotNull(message = "车位ID不能为空")
    @Schema(description = "车位ID")
    private Long spaceId;

    @NotNull(message = "车牌号码不能为空")
    @Pattern(regexp = RegexConstants.PLATE_NUMBER, message = "车牌号码格式不正确")
    @Schema(description = "车牌号码")
    private String plateNumber;

    @NotNull(message = "入场时间不能为空")
    @Schema(description = "入场时间")
    private LocalDateTime entryTime;
}
