package com.changan.park.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.changan.common.enums.CommonStatus;
import com.changan.model.dto.FacilityFormDTO;
import com.changan.model.po.ParkingFacility;
import com.changan.model.vo.FacilityItemVO;

import java.util.List;
import java.util.Map;

public interface IParkingFacilityService extends IService<ParkingFacility> {

    /**
     * 设施列表：条件均可为 null
     *
     * @param code   编码，模糊匹配
     * @param name   名称，模糊匹配
     * @param status 状态，精确匹配
     */
    List<ParkingFacility> listFacilities(String code, String name, CommonStatus status);

    void saveFacility(FacilityFormDTO dto);

    void updateFacility(FacilityFormDTO dto);

    void deleteFacility(Long id);

    Map<String, FacilityItemVO> codeItemMap();

    void validateCodes(List<String> codes);
}
