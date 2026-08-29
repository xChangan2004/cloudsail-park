package com.changan.park.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.changan.common.domain.dto.PageDTO;
import com.changan.model.po.PaymentRecord;
import com.changan.model.query.PaymentRecordQuery;
import com.changan.model.vo.PaymentRecordVO;

public interface IPaymentRecordService extends IService<PaymentRecord> {

    PageDTO<PaymentRecordVO> queryPaymentRecordPage(PaymentRecordQuery query);
}
