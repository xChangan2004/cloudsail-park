package com.changan.park.service;

import com.changan.model.dto.AppLoginDTO;
import com.changan.model.vo.AppLoginVO;

public interface IAuthService {

    AppLoginVO login(AppLoginDTO dto);

    void logout();

    void code(String phone);
}
