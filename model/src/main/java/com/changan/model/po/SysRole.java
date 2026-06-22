package com.changan.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.changan.common.enums.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_role")
@Schema(name = "系统角色实体")
public class SysRole extends BaseEntity {

  @NotBlank(message = "角色名称不能为空")
  @Schema(description = "角色名称")
  private String name;

  @NotBlank(message = "角色权限标识符不能为空")
  @Schema(description = "角色权限标识符")
  private String code;

  @Schema(description = "显示顺序")
  private Integer sort;

  @Schema(description = "状态")
  private CommonStatus status;

  @Schema(description = "备注")
  private String remark;
}
