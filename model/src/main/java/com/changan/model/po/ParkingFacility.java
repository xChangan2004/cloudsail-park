package com.changan.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.changan.common.enums.CommonStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("parking_facility")
public class ParkingFacility extends BaseEntity {

    private String code;

    private String name;

    private String icon;

    private Integer sort;

    private CommonStatus status;
}
