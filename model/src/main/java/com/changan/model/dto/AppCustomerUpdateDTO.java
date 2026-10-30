package com.changan.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "用户端修改个人信息表单")
public class AppCustomerUpdateDTO {

    @Size(min = 1, max = 20, message = "昵称需要 1-20 个字")
    @Schema(description = "昵称，不传表示不修改")
    private String nickname;

    @Schema(description = "头像URL，不传表示不修改")
    private String avatar;
}
