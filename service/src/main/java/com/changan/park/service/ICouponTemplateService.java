package com.changan.park.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.CommonStatus;
import com.changan.model.dto.CouponTemplateFormDTO;
import com.changan.model.po.CouponTemplate;
import com.changan.model.vo.CouponTemplatePageVO;
import com.changan.model.vo.CouponTemplateQuery;

public interface ICouponTemplateService extends IService<CouponTemplate> {

    void saveCouponTemplate(CouponTemplateFormDTO dto);

    void updateCouponTemplate(CouponTemplateFormDTO dto);

    void deleteCouponTemplateById(Long id);

    PageDTO<CouponTemplatePageVO> queryCouponTemplatePage(CouponTemplateQuery query);

    void updateCouponTemplateStatus(Long id, CommonStatus status);
}
