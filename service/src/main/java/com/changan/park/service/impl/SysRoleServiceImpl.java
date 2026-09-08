package com.changan.park.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changan.park.mapper.SysMenuMapper;
import com.changan.park.mapper.SysRoleMapper;
import com.changan.park.mapper.SysRoleMenuMapper;
import com.changan.park.mapper.SysUserRoleMapper;
import com.changan.park.service.ISysRoleService;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.CommonStatus;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.model.dto.AssignRoleMenuDTO;
import com.changan.model.po.SysMenu;
import com.changan.model.po.SysRole;
import com.changan.model.po.SysRoleMenu;
import com.changan.model.po.SysUserRole;
import com.changan.model.query.SysRoleQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public PageDTO<SysRole> queryRolePage(SysRoleQuery query) {
        Page<SysRole> page = lambdaQuery()
                .like(StrUtil.isNotBlank(query.getName()), SysRole::getName, query.getName())
                .eq(StrUtil.isNotBlank(query.getCode()), SysRole::getCode, query.getCode())
                .eq(query.getStatus() != null, SysRole::getStatus, query.getStatus())
                .orderByAsc(SysRole::getSort)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        return PageDTO.of(page);
    }

    @Override
    public void saveRole(SysRole sysRole) {
        // 1.校验角色Code是否存在
        boolean exists = isCodeExists(sysRole.getCode(), null);
        if (exists) {
            throw new BizIllegalException("角色权限标识符已存在");
        }
        // 2.保存角色信息
        save(sysRole);
    }

    @Override
    public void updateRole(SysRole sysRole) {
        Long id = sysRole.getId();
        String code = sysRole.getCode();
        // 1.校验角色是否存在
        SysRole role = getById(id);
        if (role == null) {
            throw new BizIllegalException("角色不存在");
        }
        // 2.校验修改后的角色Code是否被占用
        boolean exists = isCodeExists(code, id);
        if (exists) {
            throw new BizIllegalException("新修改的角色权限标识已存在");
        }
        // 3.修改角色信息
        updateById(sysRole);
    }

    @Override
    public void deleteRoleById(Long id) {
        // 1.校验角色是否存在
        SysRole role = getById(id);
        if (role == null) {
            throw new BizIllegalException("角色不存在");
        }
        // 2.检查当前角色是否其他关联
        Long count = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, id));
        if (count > 0) {
            throw new BizIllegalException("角色已被用户关联，无法删除");
        }
        // 3.删除角色
        removeById(id);
    }

    @Override
    public void batchDeleteRole(List<Long> ids) {
        // 1.校验角色是否都存在
        List<SysRole> roles = lambdaQuery()
                .in(SysRole::getId, ids)
                .list();
        if (roles.size() != ids.size()) {
            throw new BizIllegalException("部分角色不存在");
        }
        // 2.批量检查角色是否被用户关联
        Long count = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .in(SysUserRole::getRoleId, ids));
        if (count > 0) {
            throw new BizIllegalException("部分角色已被用户关联，无法删除");
        }
        // 3.批量删除角色
        removeByIds(ids);
    }

    @Override
    public List<SysRole> queryRoleList() {
        return lambdaQuery()
                .eq(SysRole::getStatus, CommonStatus.ENABLE)
                .orderByAsc(SysRole::getSort, SysRole::getCreateTime)
                .list();
    }

    @Override
    public List<Long> queryRoleMenusByRoleId(Long id) {
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .select(SysRoleMenu::getMenuId)
                        .eq(SysRoleMenu::getRoleId, id))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .distinct()
                .toList();
    }

    @Override
    @Transactional
    public void assignRole(AssignRoleMenuDTO assignDTO) {
        // 1.校验角色是否存在
        SysRole role = getById(assignDTO.getRoleId());
        if (role == null) {
            throw new BizIllegalException("角色不存在");
        }
        // 2.根据菜单id集合批量查询菜单信息
        List<SysMenu> menus = menuMapper.selectByIds(assignDTO.getMenuIds());
        if (menus.size() != assignDTO.getMenuIds().size()) {
            throw new BizIllegalException("分配的菜单存在异常");
        }
        // 3.移除原来的菜单关联
        roleMenuMapper.physicalDeleteByRoleId(role.getId());
        // 4.创建关联信息
        List<SysRoleMenu> roleMenus = new ArrayList<>(menus.size());
        for (SysMenu m : menus) {
            SysRoleMenu srm = new SysRoleMenu();
            srm.setRoleId(role.getId());
            srm.setMenuId(m.getId());
            roleMenus.add(srm);
        }
        // 5.批量插入
        roleMenuMapper.insert(roleMenus);
    }

    /**
     * 判断角色权限标识是否存在
     *
     * @param code      角色权限标识
     * @param excludeId 排除ID
     * @return true存在，false不存在
     */
    private boolean isCodeExists(String code, Long excludeId) {
        return lambdaQuery()
                .eq(SysRole::getCode, code)
                .ne(excludeId != null, SysRole::getId, excludeId)
                .exists();  // exists 比 count 性能更好
    }
}
