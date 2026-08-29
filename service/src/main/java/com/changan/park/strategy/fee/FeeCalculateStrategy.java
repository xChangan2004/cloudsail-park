package com.changan.park.strategy.fee;

import com.changan.common.enums.FeeRuleType;
import com.changan.model.dto.FeeRuleFormDTO;
import com.changan.model.po.FeeRule;

import java.math.BigDecimal;
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
     *
     * @param rule      计费规则
     * @param entryTime 入场时间
     * @param exitTime  出场时间
     * @return 停车费用
     */
    BigDecimal calculate(FeeRule rule, LocalDateTime entryTime, LocalDateTime exitTime);

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
     * 单日封顶：费用与（封顶价 × 停车天数）取较小值
     * @param fee 原始计算费用
     * @param rule 规则
     * @param totalMinutes 总停车分钟（入场到出场）
     * @return 封顶后费用
     */
    default BigDecimal applyDailyCap(BigDecimal fee, FeeRule rule, long totalMinutes) {
        // 如果没有配置有效的单日封顶，直接返回原始计算出来的费用，不做处理
        if (!hasDailyCap(rule)) {
            return fee;
        }
        // totalMinutes总停车分钟，向上取整得到停车天数（按连续停车时长算天数，不是自然日历天）
        long days = (totalMinutes + 1439) / 1440;// 向上取整天数
        return fee.min(rule.getDailyCap().multiply(BigDecimal.valueOf(days)));
    }
}
