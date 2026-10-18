package com.changan.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.changan.common.enums.FileStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file")
public class SysFile extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * OSS 对象键
     */
    private String objectKey;

    /**
     * 完整访问地址
     */
    private String url;

    /**
     * 原始文件名
     */
    private String originName;

    /**
     * 扩展名（小写，不含点）
     */
    private String ext;

    /**
     * MIME 类型
     */
    private String contentType;

    /**
     * 字节数
     */
    private Long size;

    /**
     * 0-临时 1-已使用
     */
    private FileStatus status;
}
