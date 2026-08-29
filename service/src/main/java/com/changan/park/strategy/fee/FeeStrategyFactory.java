package com.changan.park.strategy.fee;

import com.changan.common.enums.FeeRuleType;
import com.changan.common.exceptions.BizIllegalException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;

/**
 * 计费策略工厂
 */
@Component
public class FeeStrategyFactory {

    private final EnumMap<FeeRuleType, FeeCalculateStrategy> strategyMap;

    public FeeStrategyFactory(List<FeeCalculateStrategy> strategies) {
        this.strategyMap = new EnumMap<>(FeeRuleType.class);
        for (FeeCalculateStrategy strategy : strategies) {
            this.strategyMap.put(strategy.supportType(), strategy);
        }
    }

    /**
     * 根据计费类型获取对应策略
     */
    public FeeCalculateStrategy getStrategy(FeeRuleType type) {
        FeeCalculateStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new BizIllegalException("不支持的计费类型");
        }
        return strategy;
    }
}
