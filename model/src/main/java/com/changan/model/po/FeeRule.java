package com.changan.model.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.changan.common.enums.CommonStatus;
import com.changan.common.enums.FeeRuleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
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
    private FeeRuleType ruleType;

    @Schema(description = "免费时长（分钟）")
    private Integer freeMinutes;

    @Schema(description = "首小时费率")
    private BigDecimal firstHourRate;

    @Schema(description = "后续小时费率")
    private BigDecimal additionalRate;

    @Schema(description = "单日封顶金额")
    private BigDecimal dailyCap;

    @TableField(typeHandler = JacksonTypeHandler.class)
    @Schema(description = "分时段费率配置")
    private List<TimeSegment> timeSegments;

    @TableField(typeHandler = JacksonTypeHandler.class)
    @Schema(description = "阶梯费率配置")
    private List<Tier> tiers;

    @Schema(description = "状态")
    private CommonStatus status;

    @Data
    public static class TimeSegment {
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "时间格式必须为HH:mm")
        private String start;

        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "时间格式必须为HH:mm")
        private String end;

        @DecimalMin(value = "0.0", message = "时段费率不能为负数")
        private BigDecimal rate; // 该时段每小时费率
    }

    @Data
    public static class Tier {
        private Integer endMinutes; // 该阶梯结束的累计分钟数；最后一段必须为null，表示"之后无限"

        @DecimalMin(value = "0.0", message = "时段费率不能为负数")
        private BigDecimal rate; // 该时段每小时费率
    }
}
