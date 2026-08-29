package com.changan.park.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changan.park.mapper.FeeRuleMapper;
import com.changan.park.mapper.ParkingLotMapper;
import com.changan.park.service.IFeeRuleService;
import com.changan.park.strategy.fee.FeeStrategyFactory;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.CommonStatus;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.model.dto.FeeRuleFormDTO;
import com.changan.model.po.FeeRule;
import com.changan.model.po.ParkingLot;
import com.changan.model.query.FeeRuleQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeeRuleServiceImpl extends ServiceImpl<FeeRuleMapper, FeeRule> implements IFeeRuleService {

    private final ParkingLotMapper parkingLotMapper;
    private final FeeStrategyFactory feeStrategyFactory;

    @Override
    @Transactional
    public void saveFeeRule(FeeRuleFormDTO dto) {
        // 1.校验停车场是否存在
        Long lotId = dto.getLotId();
        ParkingLot lot = parkingLotMapper.selectById(lotId);
        if (lot == null) {
            throw new BizIllegalException("停车场不存在");
        }
        // 2.类型相关校验
        feeStrategyFactory.getStrategy(dto.getRuleType()).validate(dto);
        // 3.保存规则
        FeeRule rule = BeanUtil.copyProperties(dto, FeeRule.class);
        save(rule);
    }

    @Override
    public void deleteFeeRuleById(Long id) {
        // 1.校验规则是否存在
        FeeRule rule = getById(id);
        if (rule == null) {
            throw new BizIllegalException("计费规则不存在");
        }
        // 2.启用中的规则不能删除（防止正在计费的规则被删除掉）
        if (rule.getStatus() == CommonStatus.ENABLE) {
            throw new BizIllegalException("启用中的计费规则不能删除，请先停用");
        }
        // 3.删除
        removeById(id);
    }

    @Override
    public PageDTO<FeeRule> queryFeeRulePage(FeeRuleQuery query) {
        Page<FeeRule> page = lambdaQuery()
                .eq(query.getLotId() != null, FeeRule::getLotId, query.getLotId())
                .like(StrUtil.isNotBlank(query.getRuleName()), FeeRule::getRuleName, query.getRuleName())
                .eq(query.getRuleType() != null, FeeRule::getRuleType, query.getRuleType())
                .eq(query.getStatus() != null, FeeRule::getStatus, query.getStatus())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        return PageDTO.of(page);
    }

    @Override
    public FeeRule queryFeeRuleById(Long id) {
        FeeRule rule = getById(id);
        if (rule == null) {
            throw new BizIllegalException("计费规则不存在");
        }
        return rule;
    }

    @Override
    @Transactional
    public void updateFeeRule(FeeRuleFormDTO dto) {
        // 1.校验规则是否存在
        Long id = dto.getId();
        FeeRule rule = getById(id);
        if (rule == null) {
            throw new BizIllegalException("计费规则不存在");
        }
        // 2.校验停车场是否存在
        Long lotId = dto.getLotId();
        ParkingLot lot = parkingLotMapper.selectById(lotId);
        if (lot == null) {
            throw new BizIllegalException("停车场不存在");
        }
        // 3.使用中的计费规则不能修改
        if (rule.getStatus() == CommonStatus.ENABLE) {
            throw new BizIllegalException("当前计费规则正在被使用，无法修改");
        }
        // 4.类型相关数据校验
        feeStrategyFactory.getStrategy(dto.getRuleType()).validate(dto);
        // 5.修改规则信息
        BeanUtil.copyProperties(dto, rule);
        updateById(rule);
    }

    @Override
    public void updateFeeRuleStatus(Long id, CommonStatus status) {
        // 悲观锁，防止并发同时开启多条启用规则
        FeeRule rule = lambdaQuery()
                .eq(FeeRule::getId, id)
                .last("FOR UPDATE")
                .one();
        if (rule == null) {
            throw new BizIllegalException("计费规则不存在");
        }
        if (rule.getStatus() == status) {
            return; // 无效调整直接返回
        }
        // 目标状态为启用，校验同停车场唯一启用规则
        if (status == CommonStatus.ENABLE) {
            long count = lambdaQuery()
                    .eq(FeeRule::getLotId, rule.getLotId())
                    .eq(FeeRule::getStatus, CommonStatus.ENABLE)
                    .ne(FeeRule::getId, id)
                    .count();
            if (count > 0) {
                throw new BizIllegalException("该停车场已存在启用的计费规则，请先停用原规则");
            }
        }
        FeeRule updated = new FeeRule();
        updated.setId(id);
        updated.setStatus(status);
        updateById(updated);
    }

    @Override
    public FeeRule getEnableRuleByLotId(Long lotId) {
        FeeRule rule = lambdaQuery()
                .eq(FeeRule::getLotId, lotId)
                .eq(FeeRule::getStatus, CommonStatus.ENABLE)
                .last("LIMIT 1")
                .one();
        if (rule == null) {
            throw new BizIllegalException("该停车场未配置启用的计费规则，无法计算费用");
        }
        return rule;
    }
}
