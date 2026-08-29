package com.changan.park.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changan.park.mapper.CustomerMapper;
import com.changan.park.service.ICustomerPlateService;
import com.changan.park.service.ICustomerService;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.CommonStatus;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.model.dto.CustomerFormDTO;
import com.changan.model.po.Customer;
import com.changan.model.po.CustomerPlate;
import com.changan.model.query.CustomerQuery;
import com.changan.model.query.PlateQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements ICustomerService {

    private final ICustomerPlateService customerPlateService;

    // 单个客户最大绑定车牌数量
    @Value("${parking.customer-plate.max-bind-count:5}")
    private Integer maxBindCount;

    @Override
    public void saveCustomer(CustomerFormDTO dto) {
        // 1.根据手机号查询
        checkPhoneUnique(null, dto.getPhone());
        // 2.拷贝数据
        Customer customer = BeanUtil.copyProperties(dto, Customer.class);
        // 3.保存客户信息
        save(customer);
    }

    @Override
    public void updateCustomer(CustomerFormDTO dto) {
        // 1.查询客户
        Long id = dto.getId();
        Customer exists = getById(id);
        if (exists == null) {
            throw new BizIllegalException("客户不存在");
        }
        // 2.校验手机号未被使用
        checkPhoneUnique(id, dto.getPhone());
        // 3.拷贝数据
        Customer customer = BeanUtil.copyProperties(dto, Customer.class);
        // 4.更新客户信息
        updateById(customer);
    }

    @Override
    public void unbindWechat(Long id) {
        // 1.查询客户
        Customer customer = getById(id);
        if (customer == null) {
            throw new BizIllegalException("客户不存在");
        }
        // 2.检查是否关联微信
        if (StrUtil.isBlank(customer.getOpenid())) {
            throw new BizIllegalException("当前客户未关联微信，无法取消");
        }
        // 3.取消关联
        lambdaUpdate()
                .eq(Customer::getId, id)
                .set(Customer::getOpenid, null)
                .update();
    }

    @Override
    public void updateCustomerStatus(Long id, CommonStatus status) {
        // 校验客户是否存在
        if (getById(id) == null) {
            throw new BizIllegalException("客户不存在");
        }
        lambdaUpdate()
                .eq(Customer::getId, id)
                .set(Customer::getStatus, status)
                .update();
    }

    @Override
    public PageDTO<Customer> queryCustomerPage(CustomerQuery query) {
        Page<Customer> page = lambdaQuery()
                .eq(StrUtil.isNotBlank(query.getPhone()), Customer::getPhone, query.getPhone())
                .like(StrUtil.isNotBlank(query.getNickname()), Customer::getNickname, query.getNickname())
                .eq(query.getStatus() != null, Customer::getStatus, query.getStatus())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        return PageDTO.of(page);
    }

    @Override
    public void bindPlate(CustomerPlate plate) {
        // 1.查询客户信息
        Customer customer = getById(plate.getCustomerId());
        if (customer == null) {
            throw new BizIllegalException("无法将车牌绑定到不存在的客户");
        }
        plate.setPlateNumber(plate.getPlateNumber().toUpperCase());
        // 2.检查车牌是否被他人绑定
        CustomerPlate exists = customerPlateService.lambdaQuery()
                .eq(CustomerPlate::getPlateNumber, plate.getPlateNumber())
                .one();
        if (exists != null) {
            if (!exists.getCustomerId().equals(plate.getCustomerId())) {
                throw new BizIllegalException("该车牌号已被其他客户绑定");
            } else {
                throw new BizIllegalException("您已绑定该车牌，请勿重复绑定");
            }
        }
        // 3.检查是否超过绑定上限
        Long bindCount = customerPlateService.lambdaQuery()
                .eq(CustomerPlate::getCustomerId, customer.getId())
                .count();
        if (bindCount >= maxBindCount) {
            throw new BizIllegalException("最多仅可绑定" + maxBindCount + "个车牌，请先解除无用车牌");
        }
        // 如果客户还没有任何车牌，这条自动设为默认
        if (bindCount == 0) {
            plate.setIsDefault(true);
        }
        // 4.保存车牌信息
        customerPlateService.save(plate);
    }

    @Override
    @Transactional
    public void unBindPlate(Long id) {
        // 1.查询车牌信息
        CustomerPlate plate = customerPlateService.getById(id);
        if (plate == null) {
            throw new BizIllegalException("车牌记录不存在");
        }
        // 2.提前查出该客户的其余车牌（删除前查，逻辑更清晰）
        List<CustomerPlate> others = customerPlateService.lambdaQuery()
                .eq(CustomerPlate::getCustomerId, plate.getCustomerId())
                .ne(CustomerPlate::getId, id)
                .orderByDesc(CustomerPlate::getCreateTime)
                .list();
        // 3.解除车牌绑定
        customerPlateService.removeById(id);
        // 4.删除的是默认车牌时，迁移默认标识
        if (Boolean.TRUE.equals(plate.getIsDefault()) && CollUtil.isNotEmpty(others)) {
            CustomerPlate newDefault = others.get(0);
            newDefault.setIsDefault(true);
            customerPlateService.updateById(newDefault);
        }
    }

    @Override
    public List<CustomerPlate> queryCustomerPlateListByCustomerId(Long customerId) {
        List<CustomerPlate> plates = customerPlateService.lambdaQuery()
                .eq(CustomerPlate::getCustomerId, customerId)
                .list();
        if (CollUtil.isEmpty(plates)) {
            return Collections.emptyList();
        }
        return plates;
    }

    @Override
    public PageDTO<CustomerPlate> queryCustomerPlatePage(PlateQuery query) {
        Page<CustomerPlate> page = customerPlateService.lambdaQuery()
                .like(StrUtil.isNotBlank(query.getPlateName()), CustomerPlate::getPlateNumber, query.getPlateName())
                .eq(query.getEnergyType() != null, CustomerPlate::getEnergyType, query.getEnergyType())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        return PageDTO.of(page);
    }

    /**
     * 校验手机号唯一性
     *
     * @param selfId 客户id
     * @param phone  手机号码
     */
    private void checkPhoneUnique(Long selfId, String phone) {
        if (StrUtil.isBlank(phone)) {
            return;
        }
        Long count = lambdaQuery()
                .eq(Customer::getPhone, phone)
                .ne(selfId != null, Customer::getId, selfId)
                .count();
        if (count > 0) {
            throw new BizIllegalException("手机号已经被他人使用，请更换");
        }
    }
}
