package com.changan.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.CommonStatus;
import com.changan.model.dto.FeeRuleFormDTO;
import com.changan.model.po.FeeRule;
import com.changan.model.query.FeeRuleQuery;

public interface IFeeRuleService extends IService<FeeRule> {

    void saveFeeRule(FeeRuleFormDTO dto);

    void deleteFeeRuleById(Long id);

    PageDTO<FeeRule> queryFeeRulePage(FeeRuleQuery query);

    FeeRule queryFeeRuleById(Long id);

    void updateFeeRule(FeeRuleFormDTO dto);

    void updateFeeRuleStatus(Long id, CommonStatus status);

    FeeRule getEnableRuleByLotId(Long lotId);
}
