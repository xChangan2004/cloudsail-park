package com.changan.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class DashboardPlatformVO {

    @Schema(description = "今日注册数")
    private Long todayRegisterCount;

    @Schema(description = "昨日注册数")
    private Long yesterdayRegisterCount;

    @Schema(description = "累计注册数")
    private Long totalRegisterCount;

    @Schema(description = "趋势实际窗口天数")
    private Integer trendDays;

    @Schema(description = "注册趋势（近 N 天，缺数据的天补 0)")
    private List<TrendPoint> registerTrend;

    @Data
    public static class TrendPoint {

        @Schema(description = "日期")
        private String date;

        @Schema(description = "注册数")
        private Long registerCount;
    }
}
