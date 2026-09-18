package com.changan.common.converter;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import com.changan.common.enums.OperateStatus;

public class OperateStatusConverter implements Converter<OperateStatus> {

    @Override
    public Class<?> supportJavaTypeKey() {
        return OperateStatus.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    @Override
    public OperateStatus convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        String desc = cellData.getStringValue();
        if (desc == null) {
            return null;
        }
        // 根据描述文字匹配枚举
        for (OperateStatus status : OperateStatus.values()) {
            if (status.getDesc().equals(desc)) {
                return status;
            }
        }
        return null;
    }

    @Override
    public WriteCellData<?> convertToExcelData(OperateStatus value, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) throws Exception {
        if (value == null) {
            return new WriteCellData<>("");
        }
        return new WriteCellData<>(value.getDesc());
    }
}
