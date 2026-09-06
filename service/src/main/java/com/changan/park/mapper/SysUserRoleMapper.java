package com.changan.park.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.changan.model.po.SysUserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    void physicalDeleteByUserId(@Param("userId") Long userId);
}