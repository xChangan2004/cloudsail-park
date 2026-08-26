package com.changan.model.dto;

import com.changan.common.constants.RegexConstants;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExitFormDTO {

    @NotNull(message = "记录ID不能为空")
    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "离场时间（不传默认为当前时间）")
    @JsonFormat(pattern = RegexConstants.DATE_TIME_FORMAT_PATTERN, timezone = "GMT+8")
    private LocalDateTime exitTime;
}
