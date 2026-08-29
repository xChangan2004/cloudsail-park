package com.changan.park.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.changan.common.domain.dto.PageDTO;
import com.changan.model.po.SysOperateLog;
import com.changan.model.query.SysOperateLogQuery;
import com.changan.model.vo.SysOperateLogVO;

public interface ISysOperateService extends IService<SysOperateLog> {

    void saveLog(SysOperateLog logEntity);

    PageDTO<SysOperateLogVO> queryOperLogPage(SysOperateLogQuery query);
}
