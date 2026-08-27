package com.changan.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.changan.common.domain.dto.PageDTO;
import com.changan.model.po.RefundRecord;
import com.changan.model.query.RefundRecordQuery;
import com.changan.model.vo.RefundRecordVO;

public interface IRefundRecordService extends IService<RefundRecord> {

    PageDTO<RefundRecordVO> queryRefundRecordPage(RefundRecordQuery query);
}
