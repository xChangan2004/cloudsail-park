package com.changan.park.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changan.park.mapper.CustomerPlateMapper;
import com.changan.park.service.ICustomerPlateService;
import com.changan.model.po.CustomerPlate;
import org.springframework.stereotype.Service;

@Service
public class CustomerPlateServiceImpl extends ServiceImpl<CustomerPlateMapper, CustomerPlate> implements ICustomerPlateService {

}
