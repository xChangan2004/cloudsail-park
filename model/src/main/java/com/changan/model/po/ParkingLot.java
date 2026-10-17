package com.changan.model.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.changan.common.enums.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(name = "停车场实体")
@TableName(value = "parking_lot", autoResultMap = true)
public class ParkingLot extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "停车场名称")
    private String name;

    @Schema(description = "停车场地址")
    private String address;

    @Schema(description = "总车位数")
    private Integer totalSpaces;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "联系方式")
    private String contact;

    @Schema(description = "状态")
    private CommonStatus status;

    @TableField(exist = false)
    @Schema(description = "可用空位")
    private Integer freeSpaces;

    private String cover;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> facilityCodes;
}
