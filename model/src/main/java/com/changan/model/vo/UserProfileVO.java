package com.changan.model.vo;

import com.changan.common.enums.Sex;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "当前登录用户资料")
public class UserProfileVO {

    @Schema(description = "用户ID", example = "10001")
    private Long id;

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "用户昵称", example = "长安")
    private String nickname;

    @Schema(description = "手机号码", example = "13400000001")
    private String phone;

    @Schema(description = "性别")
    private Sex sex;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "角色名称集合")
    private List<String> roles;

    @Schema(description = "最近登录IP", example = "127.0.0.1")
    private String loginIp;

    @Schema(description = "最近登录时间")
    private LocalDateTime loginDate;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
