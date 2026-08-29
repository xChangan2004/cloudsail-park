package com.changan.model.vo;

import com.changan.common.enums.PayMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PayMethodStatVO {

    @Schema(description = "缴费方式占比列表")
    private List<MethodItem> methods;

    @Schema(description = "累计退款金额")
    private BigDecimal totalRefund;

    @Data
    public static class MethodItem {

        @Schema(description = "支付方式")
        private PayMethod method;

        @Schema(description = "支付方式描述")
        private String methodDesc;

        @Schema(description = "支付笔数")
        private Long count;

        @Schema(description = "支付总额")
        private BigDecimal amount;    // 支付总额
    }
}