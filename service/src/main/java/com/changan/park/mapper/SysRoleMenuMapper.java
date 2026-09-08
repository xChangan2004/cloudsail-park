package com.changan.park.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.changan.model.po.SysRoleMenu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    void physicalDeleteByRoleId(@Param("roleId") Long roleId);
}