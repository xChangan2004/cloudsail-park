package com.changan.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changan.admin.mapper.PaymentRecordMapper;
import com.changan.admin.service.IPaymentRecordService;
import com.changan.model.po.PaymentRecord;
import org.springframework.stereotype.Service;

@Service
public class PaymentRecordServiceImpl extends ServiceImpl<PaymentRecordMapper, PaymentRecord> implements IPaymentRecordService {

}
