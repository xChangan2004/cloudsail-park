package com.changan.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "微信登录/绑定微信表单")
public class WxLoginDTO {

    @NotBlank(message = "登录凭证不能为空")
    @Schema(description = "uni.login 获取的 code")
    private String code;
}