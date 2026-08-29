package com.changan.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardTrendVO {

    @Schema(description = "当天日期")
    private String date;              // 如 "2026-08-27"

    @Schema(description = "当天收入")
    private BigDecimal income;        // 当天收入（按 paid_time）

    @Schema(description = "当天入场数")
    private Long entryCount;          // 当天入场数

    @Schema(description = "当天出场数")
    private Long exitCount;           // 当天出场数
}
