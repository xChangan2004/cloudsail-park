package com.changan.park.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.changan.common.domain.dto.PageDTO;
import com.changan.model.dto.EntryFormDTO;
import com.changan.model.dto.ExitFormDTO;
import com.changan.model.po.EntryExitRecord;
import com.changan.model.query.EntryExitRecordQuery;
import com.changan.model.vo.EntryExitRecordPageVO;
import com.changan.model.vo.ExitBillVO;
import com.changan.model.vo.MyParkingStatusVO;

public interface IEntryExitRecordService extends IService<EntryExitRecord> {

    void entry(EntryFormDTO dto);

    PageDTO<EntryExitRecordPageVO> queryRecordPage(EntryExitRecordQuery query);

    ExitBillVO exit(ExitFormDTO dto);

    MyParkingStatusVO queryMyParkingStatus();
}
