package com.changan.common.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.WriteSheet;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public class ExcelUtil<T> {

    private final Class<T> clazz;

    public ExcelUtil(Class<T> clazz) {
        this.clazz = clazz;
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单（EasyExcel）
     *
     * @param list      导出数据集合
     * @param sheetName 工作表的名称
     */
    public void exportEasyExcel(HttpServletResponse response, List<T> list, String sheetName) {
        // 设置下载响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        // 导出数据
        try {
            String fileName = URLEncoder.encode(sheetName, StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream(), clazz)
                    .autoCloseStream(Boolean.FALSE)
                    .sheet(sheetName)
                    .doWrite(list);
        } catch (IOException e) {
            log.error("导出EasyExcel异常{}", e.getMessage());
        }
    }
}
