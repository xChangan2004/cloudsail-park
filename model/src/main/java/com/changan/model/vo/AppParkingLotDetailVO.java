package com.changan.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "用户端停车场详情")
public class AppParkingLotDetailVO extends AppParkingLotVO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "轮播图")
    private List<String> images;

    @Schema(description = "联系方式")
    private String contact;

    @Schema(description = "当前启用的计费规则，未配置时为 null")
    private AppFeeRuleVO feeRule;
}
