package com.changan.park.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.CommonStatus;
import com.changan.model.dto.UserFormDTO;
import com.changan.model.dto.UserLoginDTO;
import com.changan.model.dto.UserResetPwdDTO;
import com.changan.model.po.SysUser;
import com.changan.model.query.SysUserQuery;
import com.changan.model.vo.LoginVO;
import com.changan.model.vo.UserDetailVO;
import com.changan.model.vo.UserPageVO;

public interface ISysUserService extends IService<SysUser> {

    LoginVO login(UserLoginDTO loginDTO);

    void logout();

    UserDetailVO queryUserById(Long id);

    PageDTO<UserPageVO> queryUserPage(SysUserQuery query);

    void updateUserStatus(Long id, CommonStatus status);

    void resetPwd(UserResetPwdDTO dto);

    void saveUser(UserFormDTO dto);

    void deleteUserById(Long id);

    void updateUser(UserFormDTO dto);
}
