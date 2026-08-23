package com.changan.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.CommonStatus;
import com.changan.model.dto.ParkingLotFormDTO;
import com.changan.model.po.ParkingLot;
import com.changan.model.query.ParkingLotQuery;

public interface IParkingLotService extends IService<ParkingLot> {

    void saveParkingLot(ParkingLotFormDTO dto);

    PageDTO<ParkingLot> queryParkingLotPage(ParkingLotQuery query);

    ParkingLotFormDTO queryParkingLotById(Long id);

    void updateParkingLot(ParkingLotFormDTO dto);

    void deleteParkingLotById(Long id);

    void updateParkingLotStatus(Long id, CommonStatus status);
}
