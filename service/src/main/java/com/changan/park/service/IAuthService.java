package com.changan.park.service;

import com.changan.model.dto.AppLoginDTO;
import com.changan.model.dto.BindPhoneDTO;
import com.changan.model.dto.BindPhoneWxDTO;
import com.changan.model.dto.WxLoginDTO;
import com.changan.model.vo.AppLoginVO;

public interface IAuthService {

    AppLoginVO login(AppLoginDTO dto);

    AppLoginVO wxLogin(WxLoginDTO dto);

    void bindPhone(BindPhoneDTO dto);

    String bindPhoneByWxCode(BindPhoneWxDTO dto);

    void bindWx(WxLoginDTO dto);

    void unbindWx();

    void logout();

    void code(String phone);
}
