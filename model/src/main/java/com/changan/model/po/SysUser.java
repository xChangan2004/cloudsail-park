package com.changan.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.changan.common.enums.CommonStatus;
import com.changan.common.enums.Sex;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
@Schema(name = "系统用户实体")
public class SysUser extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号码")
    private String phone;

    @Schema(description = "性别")
    private Sex sex;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "状态")
    private CommonStatus status;

    @Schema(description = "最后登录IP")
    private String loginIp;

    @Schema(description = "最后登录时间")
    private LocalDateTime loginDate;
}
