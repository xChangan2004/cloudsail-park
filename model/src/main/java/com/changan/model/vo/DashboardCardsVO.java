package com.changan.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardCardsVO {

    @Schema(description = "今日收入")
    private BigDecimal todayIncome;

    @Schema(description = "今日入场数")
    private Long todayEntryCount;

    @Schema(description = "今日出场数")
    private Long todayExitCount;

    @Schema(description = "车位总数")
    private Long totalSpaces;

    @Schema(description = "占用车位数")
    private Long occupiedSpaces;

    @Schema(description = "空闲车位数")
    private Long freeSpaces;
}
