package com.changan.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.CommonStatus;
import com.changan.common.enums.ParkingSpaceStatus;
import com.changan.model.po.ParkingSpace;
import com.changan.model.query.ParkingSpaceQuery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface IParkingSpaceService extends IService<ParkingSpace> {

    PageDTO<ParkingSpace> queryParkingSpacePage(ParkingSpaceQuery query);

    void saveParkingSpace(ParkingSpace parkingSpace);

    void saveParkingSpaceBatch(List<ParkingSpace> parkingSpaces);

    ParkingSpace getParkingSpace(Long id);

    void deleteParkingSpace(Long id);

    void updateParkingSpaceStatus(Long id, ParkingSpaceStatus status);
}
