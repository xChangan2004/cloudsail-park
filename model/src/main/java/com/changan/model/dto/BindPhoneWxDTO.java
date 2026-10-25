package com.changan.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "微信登录后绑定手机号表单（微信一键获取手机号方式）")
public class BindPhoneWxDTO {

    @NotBlank(message = "手机号凭证不能为空")
    @Schema(description = "getPhoneNumber 回调里的 code")
    private String wxCode;
}
