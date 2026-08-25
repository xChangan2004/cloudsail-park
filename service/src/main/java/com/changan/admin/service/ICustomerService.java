package com.changan.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.CommonStatus;
import com.changan.model.dto.CustomerFormDTO;
import com.changan.model.po.Customer;
import com.changan.model.po.CustomerPlate;
import com.changan.model.query.CustomerQuery;
import com.changan.model.query.PlateQuery;

import java.util.List;

public interface ICustomerService extends IService<Customer> {

    void saveCustomer(CustomerFormDTO dto);

    void updateCustomer(CustomerFormDTO dto);

    void unbindWechat(Long id);

    void updateCustomerStatus(Long id, CommonStatus status);

    PageDTO<Customer> queryCustomerPage(CustomerQuery query);

    void bindPlate(CustomerPlate plate);

    void unBindPlate(Long id);

    List<CustomerPlate> queryCustomerPlateListByCustomerId(Long customerId);

    PageDTO<CustomerPlate> queryCustomerPlatePage(PlateQuery query);
}
