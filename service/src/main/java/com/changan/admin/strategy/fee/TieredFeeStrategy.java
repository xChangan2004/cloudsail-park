package com.changan.admin.strategy.fee;

import cn.hutool.core.collection.CollUtil;
import com.changan.common.enums.FeeRuleType;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.model.dto.FeeRuleFormDTO;
import com.changan.model.po.FeeRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 阶梯计费策略：按累计停车时长分段累进计费，每段按分钟比例折算
 * <p>示例：0~120分钟5元/小时，120~300分钟3元/小时，之后1元/小时</p>
 */
@Component
public class TieredFeeStrategy implements FeeCalculateStrategy {

    @Override
    public FeeRuleType supportType() {
        return FeeRuleType.TIERED;
    }

    @Override
    public void validate(FeeRuleFormDTO dto) {
        List<FeeRule.Tier> tiers = dto.getTiers();
        if (CollUtil.isEmpty(tiers)) {
            throw new BizIllegalException("阶梯计费必须配置阶梯费率");
        }
        for (FeeRule.Tier tier : tiers) {
            if (tier.getRate() == null) {
                throw new BizIllegalException("阶梯费率不能为空");
            }
        }
        // 最后一段必须是无限档（endMinutes=null)
        if (tiers.get(tiers.size() - 1).getEndMinutes() != null) {
            throw new BizIllegalException("最后一个阶梯的结束分钟数必须为空（表示之后不限时长）");
        }
        // 前面的档位必须有结束分钟数，且严格递增
        Integer prevEnd = 0;
        for (int i = 0; i < tiers.size() - 1; i++) {
            Integer end = tiers.get(i).getEndMinutes();
            if (end == null) {
                throw new BizIllegalException("只有最后一个阶梯的结束分钟才能为空");
            }
            if (end <= prevEnd) {
                throw new BizIllegalException("阶梯结束分钟必须严格递增");
            }
            prevEnd = end;
        }
    }

    @Override
    public BigDecimal calculate(FeeRule rule, LocalDateTime entryTime, LocalDateTime exitTime) {
        // 1、计算停车总时长（单位：分钟，丢弃秒部分）
        long totalMinutes = Duration.between(entryTime, exitTime).toMinutes();
        // 2、总停车时长小于等于免费时长，直接0元
        if (totalMinutes <= rule.getFreeMinutes()) {
            return BigDecimal.ZERO;
        }
        // 3、扣除免费分钟，得到真正需要参与计费的分钟数
        long billable = totalMinutes - rule.getFreeMinutes();
        // 4、游标切片计算累进阶梯费用：类似个税累进计算，按时间片段分别计价累加
        BigDecimal fee = BigDecimal.ZERO;
        // cursor：时间游标，代表【已经完成计费】的时间点，初始从0开始
        long cursor = 0;
        for (FeeRule.Tier tier : rule.getTiers()) {
            /*
             * end：当前阶梯实际截止时间
             * 如果是无上限的最后一档(endMinutes=null)：直接取全部计费分钟billable，吃掉剩余所有时间
             * 普通档位：取本档配置结束分钟 和 billable 的较小值，不能超出总计费时长
             */
            long end = tier.getEndMinutes() == null ? billable :
                    Math.min(tier.getEndMinutes(), billable);
            // end <= cursor：本阶梯区间已经被消耗完毕，没有可计费时长，跳过本档
            if (end <= cursor) {
                continue;
            }
            // end‑cursor：当前阶梯内需要计费的分钟长度
            // rate * 分钟 /60：小时单价折算该片段费用；每段四舍五入保留2位小数
            fee = fee.add(tier.getRate()
                    .multiply(BigDecimal.valueOf(end - cursor))
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));

            // 游标向前推进到本区间终点，标记这一段时间已经计费完成
            cursor = end;
            // 游标已经走完全部计费时间，剩余阶梯无需遍历，直接跳出循环
            if (cursor >= billable) {
                break;
            }
        }
        // 5、应用单日封顶逻辑：计算出来的费用不能超过单日封顶金额
        return applyDailyCap(fee, rule, totalMinutes);
    }

    @Override
    public String getRuleDesc(FeeRule rule) {
        if (CollUtil.isEmpty(rule.getTiers())) {
            return "阶梯计费";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("免费").append(rule.getFreeMinutes()).append("分钟，");
        List<FeeRule.Tier> tiers = rule.getTiers();
        for (int i = 0; i < tiers.size(); i++) {
            FeeRule.Tier tier = tiers.get(i);
            if (i > 0) {
                sb.append("，");
            }
            if (tier.getEndMinutes() == null) {
                sb.append("之后每小时").append(tier.getRate()).append("元");
            } else {
                sb.append(i == 0 ? "前" : "").append(tier.getEndMinutes()).append("分钟内每小时")
                        .append(tier.getRate()).append("元");
            }
        }
        if (hasDailyCap(rule)) {
            sb.append("，单日封顶").append(rule.getDailyCap()).append("元");
        }
        return sb.toString();
    }
}
