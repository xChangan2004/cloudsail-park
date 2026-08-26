package com.changan.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.changan.common.enums.EntryExitStatus;
import com.changan.common.enums.EntryType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("entry_exit_record")
@Schema(description = "出入场地记录实体")
public class EntryExitRecord extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "停车场ID")
    private Long lotId;

    @Schema(description = "车位ID")
    private Long spaceId;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "车牌号码")
    private String plateNumber;

    @Schema(description = "入场类型")
    private EntryType entryType;

    @Schema(description = "入场时间")
    private LocalDateTime entryTime;

    @Schema(description = "出场时间")
    private LocalDateTime exitTime;

    @Schema(description = "入场图片URL")
    private String entryImage;

    @Schema(description = "出场图片URL")
    private String exitImage;

    @Schema(description = "出入记录状态")
    private EntryExitStatus status;
}
