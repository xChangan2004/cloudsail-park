package com.changan.park.strategy.fee;

import com.changan.common.enums.FeeRuleType;
import com.changan.model.dto.FeeRuleFormDTO;
import com.changan.model.po.FeeRule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 计费策略接口
 * <p>每种计费类型对应一个实现类，负责该类型从配置校验到费用计算的全部逻辑</p>
 */
public interface FeeCalculateStrategy {

    /**
     * 当前策略支持的计费类型
     */
    FeeRuleType supportType();

    /**
     * 校验规则配置（新增/修改计费规则时调用）
     *
     * @param dto 规则表单参数
     */
    void validate(FeeRuleFormDTO dto);

    /**
     * 计算停车费用（出场时调用）
     * <p>先算原始费用，再以「连续 24 小时」为一段逐段应用单日封顶后累加。</p>
     *
     * @param rule      计费规则
     * @param entryTime 入场时间
     * @param exitTime  出场时间
     * @return 停车费用
     */
    default BigDecimal calculate(FeeRule rule, LocalDateTime entryTime, LocalDateTime exitTime) {
        if (!exitTime.isAfter(entryTime)) {
            return BigDecimal.ZERO;
        }
        if (!hasDailyCap(rule)) {
            return calculateRaw(rule, entryTime, exitTime);
        }
        // 不能直接对总费用做 min(总费用, 封顶 × 天数)：
        // 停车 24 小时零 1 分钟时天数会从 1 跳到 2，费用在临界点直接突变。
        // 这里以入场时间起算的连续 24 小时为一段，每段增量费用单独封顶后累加。
        BigDecimal cap = rule.getDailyCap();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal prevRaw = BigDecimal.ZERO;
        LocalDateTime cursor = entryTime;
        while (cursor.isBefore(exitTime)) {
            LocalDateTime windowEnd = cursor.plusDays(1);
            if (windowEnd.isAfter(exitTime)) {
                windowEnd = exitTime;
            }
            BigDecimal raw = calculateRaw(rule, entryTime, windowEnd);
            BigDecimal windowFee = raw.subtract(prevRaw).max(BigDecimal.ZERO).min(cap);
            total = total.add(windowFee);
            prevRaw = raw;
            cursor = windowEnd;
        }
        return total;
    }

    /**
     * 计算不考虑单日封顶的原始费用
     *
     * @param rule      计费规则
     * @param entryTime 入场时间
     * @param exitTime  出场时间
     * @return 原始停车费用
     */
    BigDecimal calculateRaw(FeeRule rule, LocalDateTime entryTime, LocalDateTime exitTime);

    /**
     * 生成规则的展示文案
     *
     * @param rule 计费规则
     * @return 如 "免费15分钟，首小时5.00元，之后每小时3.00元，单日封顶50.00元"
     */
    String getRuleDesc(FeeRule rule);

    /**
     * 是否存在单日封顶
     */
    default boolean hasDailyCap(FeeRule rule) {
        return rule.getDailyCap() != null && rule.getDailyCap().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 是否配置了免费时长
     */
    default boolean hasFreeMinutes(FeeRule rule) {
        return rule.getFreeMinutes() != null && rule.getFreeMinutes() > 0;
    }

    /**
     * 金额文案：统一保留两位小数，避免出现 5.0 元 / 8 元 这类不统一展示
     */
    default String formatMoney(BigDecimal amount) {
        return amount == null ? "0.00" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
