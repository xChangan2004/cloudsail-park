package com.changan.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.changan.common.domain.dto.PageDTO;
import com.changan.model.dto.AssignRoleMenuDTO;
import com.changan.model.po.SysRole;
import com.changan.model.query.SysRoleQuery;

import java.util.List;

public interface ISysRoleService extends IService<SysRole> {

    PageDTO<SysRole> queryRolePage(SysRoleQuery query);

    void saveRole(SysRole sysRole);

    void updateRole(SysRole sysRole);

    void deleteRoleById(Long id);

    void batchDeleteRole(List<Long> ids);

    List<SysRole> queryRoleList();

    List<Long> queryRoleMenusByRoleId(Long id);

    void assignRole(AssignRoleMenuDTO assignDTO);
}
