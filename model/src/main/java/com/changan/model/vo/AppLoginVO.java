package com.changan.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户端登录结果")
public class AppLoginVO {

    @Schema(description = "登录token，后续请求放在请求头 Authorization 中")
    private String token;

    @Schema(description = "用户ID")
    private Long customerId;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "手机号码")
    private String phone;

    @Schema(description = "是否新注册用户（true=本次登录自动注册）")
    private Boolean isNewUser;
}