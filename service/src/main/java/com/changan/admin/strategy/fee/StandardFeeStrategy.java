package com.changan.admin.strategy.fee;


import com.changan.common.enums.FeeRuleType;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.model.dto.FeeRuleFormDTO;
import com.changan.model.po.FeeRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 标准计费策略：首小时 + 后续小时，不足1小时向上取整
 */
@Component
public class StandardFeeStrategy implements FeeCalculateStrategy {

    @Override
    public FeeRuleType supportType() {
        return FeeRuleType.STANDARD;
    }

    @Override
    public void validate(FeeRuleFormDTO dto) {
        if (dto.getFirstHourRate() == null || dto.getAdditionalRate() == null) {
            throw new BizIllegalException("标准计费必须配置首以及后续小时费率");
        }
    }

    @Override
    public BigDecimal calculate(FeeRule rule, LocalDateTime entryTime, LocalDateTime exitTime) {
        // 1.计算出场和离场相差的分钟
        long totalMinutes = Duration.between(entryTime, exitTime).toMinutes();
        // 2.免费时间内（0元）
        if (totalMinutes <= rule.getFreeMinutes()) {
            return BigDecimal.ZERO;
        }
        // 3.首小时 + 之后每小时（不足1小时按1小时算）
        long billable = totalMinutes - rule.getFreeMinutes();
        long hours = (billable + 59) / 60;
        BigDecimal fee = rule.getFirstHourRate()
                .add(rule.getAdditionalRate().multiply(BigDecimal.valueOf(hours - 1)));
        // 4.单日封顶
        return applyDailyCap(fee, rule, totalMinutes);
    }

    @Override
    public String getRuleDesc(FeeRule rule) {
        StringBuilder sb = new StringBuilder();
        sb.append("免费").append(rule.getFreeMinutes()).append("分钟，");
        sb.append("首小时").append(rule.getFirstHourRate()).append("元，");
        sb.append("之后每小时").append(rule.getAdditionalRate()).append("元");
        if (hasDailyCap(rule)) {
            sb.append("，单日封顶").append(rule.getDailyCap()).append("元");
        }
        return sb.toString();
    }
}
