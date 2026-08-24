package com.changan.model.query;

import com.changan.common.domain.query.PageQuery;
import com.changan.common.enums.CommonStatus;
import com.changan.common.enums.FeeRuleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Schema(description = "计费规则分页查询条件")
public class FeeRuleQuery extends PageQuery {

    @Schema(description = "停车场ID")
    private Long lotId;

    @Schema(description = "规则名称（模糊查询）")
    private String ruleName;

    @Schema(description = "规则类型")
    private FeeRuleType ruleType;

    @Schema(description = "状态")
    private CommonStatus status;
}
