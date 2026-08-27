package com.changan.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changan.admin.mapper.RefundRecordMapper;
import com.changan.admin.service.IRefundRecordService;
import com.changan.model.po.RefundRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IRefundRecordServiceImpl extends ServiceImpl<RefundRecordMapper, RefundRecord> implements IRefundRecordService {

}
