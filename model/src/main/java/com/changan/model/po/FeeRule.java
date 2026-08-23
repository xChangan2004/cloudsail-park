package com.changan.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.changan.common.enums.CommonStatus;
import com.changan.common.enums.RuleFeeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "fee_rule", autoResultMap = true)
@Schema(name = "计费规则实体")
public class FeeRule extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "停车场ID")
    private Long lotId;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "规则类型")
    private RuleFeeType ruleType;

    @Schema(description = "免费时长（分钟）")
    private Integer freeMinutes;

    @Schema(description = "首小时费率")
    private BigDecimal firstHourRate;

    @Schema(description = "后续小时费率")
    private BigDecimal additionalDate;

    @Schema(description = "单日封顶金额")
    private BigDecimal dailyCap;

    @Schema(description = "分时段费率配置")
    private List<TimeSegment> timeSegments;

    @Schema(description = "状态")
    private CommonStatus status;

    @Data
    public static class TimeSegment {
        private String start;
        private String end;
        private BigDecimal rate; // 该时段每小时费率
    }
}
