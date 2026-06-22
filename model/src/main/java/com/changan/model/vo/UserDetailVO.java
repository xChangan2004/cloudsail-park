package com.changan.model.vo;

import com.changan.common.enums.CommonStatus;
import com.changan.common.enums.Sex;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "用户详情 VO（编辑回显用）")
public class UserDetailVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "用户性别")
    private Sex sex;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "状态")
    private CommonStatus status;

    @Schema(description = "关联的角色 ID 列表")
    private List<Long> roleIds;
}