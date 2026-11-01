package com.changan.model.vo;

import com.changan.common.enums.FeeRuleType;
import com.changan.model.po.FeeRule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "用户端计费规则VO")
public class AppFeeRuleVO {

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "计费类型，1-标准 2-时段 3-阶梯")
    private FeeRuleType ruleType;

    @Schema(description = "免费时长（分钟）")
    private Integer freeMinutes;

    @Schema(description = "首小时费率")
    private BigDecimal firstHourRate;

    @Schema(description = "后续小时费率")
    private BigDecimal additionalRate;

    @Schema(description = "每 24 小时封顶金额")
    private BigDecimal dailyCap;

    @Schema(description = "分时段费率，ruleType=2 时有值")
    private List<FeeRule.TimeSegment> timeSegments;

    @Schema(description = "阶梯费率，ruleType=3 时有值")
    private List<FeeRule.Tier> tiers;

    public static AppFeeRuleVO of(FeeRule rule) {
        if (rule == null) {
            return null;
        }
        AppFeeRuleVO vo = new AppFeeRuleVO();
        vo.setRuleName(rule.getRuleName());
        vo.setRuleType(rule.getRuleType());
        vo.setFreeMinutes(rule.getFreeMinutes());
        vo.setFirstHourRate(rule.getFirstHourRate());
        vo.setAdditionalRate(rule.getAdditionalRate());
        vo.setDailyCap(rule.getDailyCap());
        vo.setTimeSegments(rule.getTimeSegments());
        vo.setTiers(rule.getTiers());
        return vo;
    }
}
