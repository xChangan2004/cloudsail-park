package com.changan.park.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.CommonStatus;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.model.dto.CouponTemplateFormDTO;
import com.changan.model.po.CouponTemplate;
import com.changan.model.vo.CouponTemplatePageVO;
import com.changan.model.vo.CouponTemplateQuery;
import com.changan.park.mapper.CouponTemplateMapper;
import com.changan.park.service.ICouponTemplateService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplate> implements ICouponTemplateService {

    @Override
    public void saveCouponTemplate(CouponTemplateFormDTO dto) {
        if (dto.getThreshold().compareTo(BigDecimal.ZERO) > 0
                && dto.getAmount().compareTo(dto.getThreshold()) >= 0) {
            throw new BizIllegalException("抵扣金额必须小于使用门槛");
        }
        CouponTemplate template = BeanUtil.copyProperties(dto, CouponTemplate.class);
        template.setIssuedCount(0);
        template.setStatus(CommonStatus.DISABLE);
        // 落库
        save(template);
    }

    @Override
    public void updateCouponTemplate(CouponTemplateFormDTO dto) {
        // 1.校验ID参数
        if (dto.getId() == null) {
            throw new BizIllegalException("模板ID不能为空");
        }
        // 2.跨字段校验
        if (dto.getThreshold().compareTo(BigDecimal.ZERO) > 0
                && dto.getAmount().compareTo(dto.getThreshold()) >= 0) {
            throw new BizIllegalException("抵扣金额必须小于使用门槛");
        }
        // 3.已发放的模板不可修改
        CouponTemplate template = getById(dto.getId());
        if (template == null) {
            throw new BizIllegalException("优惠券模板不存在");
        }
        if (template.getIssuedCount() > 0) {
            throw new BizIllegalException("已发放的模板不能修改，请新建模板代替");
        }
        // 4.更新
        CouponTemplate updated = BeanUtil.copyProperties(dto, CouponTemplate.class);
        updated.setIssuedCount(0);
        updated.setStatus(CommonStatus.DISABLE);
        updateById(updated);
    }

    @Override
    public void deleteCouponTemplateById(Long id) {
        // 1.检查模板是否存在
        CouponTemplate template = getById(id);
        if (template == null) {
            throw new BizIllegalException("优惠券模板不存在");
        }
        // 2.已发放的不能删
        if (template.getIssuedCount() > 0) {
            throw new BizIllegalException("已发放的模板不能删除");
        }
        // 3.上架中的不能删
        if (template.getStatus() == CommonStatus.ENABLE) {
            throw new BizIllegalException("上架中的模板不能删除，请先下架");
        }
        // 4.删除
        removeById(id);
    }

    @Override
    public PageDTO<CouponTemplatePageVO> queryCouponTemplatePage(CouponTemplateQuery query) {
        Page<CouponTemplate> page = lambdaQuery()
                .like(StrUtil.isNotBlank(query.getName()), CouponTemplate::getName, query.getName())
                .eq(query.getStatus() != null, CouponTemplate::getStatus, query.getStatus())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<CouponTemplate> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        return PageDTO.of(page, t -> {
            CouponTemplatePageVO vo = BeanUtil.copyProperties(t, CouponTemplatePageVO.class);
            // 领取进度百分比
            vo.setReceivedRate(t.getTotalCount() == 0 ? 0 :
                    t.getIssuedCount() * 100 / t.getTotalCount());
            return vo;
        });
    }

    @Override
    public void updateCouponTemplateStatus(Long id, CommonStatus status) {
        // 1.校验模板是否存在
        CouponTemplate template = getById(id);
        if (template == null) {
            throw new BizIllegalException("优惠券模板不存在");
        }
        // 2.状态相同直接返回
        if (template.getStatus() == status) {
            return;
        }
        // 3.更新状态
        boolean updated = lambdaUpdate()
                .eq(CouponTemplate::getId, id)
                .eq(CouponTemplate::getStatus, template.getStatus())
                .set(CouponTemplate::getStatus, status)
                .update();
        if(!updated) {
            throw new BizIllegalException("操作失败，请刷新后重试");
        }
    }
}
