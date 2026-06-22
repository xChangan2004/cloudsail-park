package com.changan.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changan.admin.mapper.SysMenuMapper;
import com.changan.admin.mapper.SysRoleMenuMapper;
import com.changan.admin.service.ISysMenuService;
import com.changan.common.enums.MenuType;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.common.utils.TreeUtils;
import com.changan.model.po.SysMenu;
import com.changan.model.po.SysRoleMenu;
import com.changan.model.query.SysMenuQuery;
import com.changan.model.vo.MenuSimpleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements ISysMenuService {

    private final SysRoleMenuMapper roleMenuMapper;

    @Override
    public List<MenuSimpleVO> queryMenuList(SysMenuQuery query) {
        List<SysMenu> menus = lambdaQuery()
                .like(StrUtil.isNotBlank(query.getName()), SysMenu::getName, query.getName())
                .eq(query.getStatus() != null, SysMenu::getStatus, query.getStatus())
                .eq(query.getType() != null, SysMenu::getType, query.getType())
                .list();
        List<MenuSimpleVO> voList = menus.stream()
                .map(this::toMenuSimpleVO)
                .toList();
        if (StrUtil.isBlank(query.getName()) && query.getStatus() == null && query.getType() == null) {
            // 无查询条件，返回树
            return TreeUtils.buildTree(voList, MenuSimpleVO::getId, MenuSimpleVO::getParentId, MenuSimpleVO::setChildren);
        } else {
            // 有查询条件，返回扁平列表
            return voList;
        }
    }

    @Override
    public void deleteMenuById(Long id) {
        // 1.菜单存在校验
        SysMenu menu = getById(id);
        if (menu == null) {
            throw new BizIllegalException("菜单不存在");
        }
        // 2.查询是否有子菜单
        Long childCount = lambdaQuery()
                .eq(SysMenu::getParentId, id)
                .count();
        if (childCount > 0) {
            throw new BizIllegalException("该菜单下存在子菜单，请先删除子菜单");
        }
        // 3.查询是否被角色关联
        Long roleMenuCount = roleMenuMapper.selectCount(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getMenuId, id));
        if (roleMenuCount > 0) {
            throw new BizIllegalException("该菜单正在被角色关联，请取消关联");
        }
        // 4.删除
        removeById(id);
    }

    @Override
    public void saveMenu(SysMenu sysMenu) {
        // 1.校验 parentId 是否存在
        if (sysMenu.getParentId() != null && !sysMenu.getParentId().equals(0L)) {
            // 存在，检查父菜单是否存在
            boolean exists = lambdaQuery()
                    .eq(SysMenu::getId, sysMenu.getParentId())
                    .exists();
            if (!exists) {
                throw new BizIllegalException("关联的父菜单不存在");
            }
        }
        // 2.校验同级下path不能重复
        if (StrUtil.isNotBlank(sysMenu.getPath())) {
            Long count = lambdaQuery()
                    .eq(SysMenu::getPath, sysMenu.getPath())
                    .eq(SysMenu::getParentId, sysMenu.getParentId())
                    .count();
            if (count > 0) {
                throw new BizIllegalException("同级下菜单path不能重复");
            }
        }
        // 3.如果是菜单/目录，那么path是必填项
        if ((sysMenu.getType() == MenuType.CATALOGUE ||
                sysMenu.getType() == MenuType.MENU) && StrUtil.isBlank(sysMenu.getPath())) {
            throw new BizIllegalException("目录/菜单的path路由地址必填");
        }
        // 4.如果是按钮，那么permission是必填项
        if (sysMenu.getType() == MenuType.BUTTON && StrUtil.isBlank(sysMenu.getPermission())) {
            throw new BizIllegalException("按钮permission权限标识必填");
        }
        // 5.保存菜单
        save(sysMenu);
    }

    @Override
    public void updateMenu(SysMenu sysMenu) {
        Long id = sysMenu.getId();
        Long parentId = sysMenu.getParentId();

        // 1.菜单存在校验
        boolean exists = lambdaQuery().eq(SysMenu::getId, id).exists();
        if (!exists) {
            throw new BizIllegalException("当前修改的菜单不存在");
        }
        // 2.父菜单不能是自己
        if (parentId != null && parentId.equals(id)) {
            throw new BizIllegalException("父菜单不能选择自己");
        }
        // 3.父菜单不能是自己的子节点（防止形成循环树）
        if (parentId != null && !parentId.equals(0L)) {
            // 查询所有子节点（包括孙子、重孙...）
            List<Long> childIds = getChildIdsRecursive(id);
            if (childIds.contains(parentId)) {
                throw new BizIllegalException("父菜单不能选择自己的子节点");
            }

            // 4. 校验父菜单真实存在
            boolean parentExists = lambdaQuery().eq(SysMenu::getId, parentId).exists();
            if (!parentExists) {
                throw new BizIllegalException("父菜单不存在");
            }
        }
        // 5. 校验同级 path 不重复（排除自己）
        if (StrUtil.isNotBlank(sysMenu.getPath())) {
            Long count = lambdaQuery()
                    .eq(SysMenu::getPath, sysMenu.getPath())
                    .eq(SysMenu::getParentId, parentId)
                    .ne(SysMenu::getId, id)
                    .count();
            if (count > 0) {
                throw new BizIllegalException("同级下菜单path不能重复");
            }
        }
        // 6. 菜单类型规则校验
        if ((sysMenu.getType() == MenuType.CATALOGUE || sysMenu.getType() == MenuType.MENU)
                && StrUtil.isBlank(sysMenu.getPath())) {
            throw new BizIllegalException("目录/菜单的path必填");
        }
        if (sysMenu.getType() == MenuType.BUTTON
                && StrUtil.isBlank(sysMenu.getPermission())) {
            throw new BizIllegalException("按钮权限标识permission必填");
        }

        // 7. 执行更新
        updateById(sysMenu);
    }

    @Override
    public SysMenu queryMenuById(Long id) {
        return getById(id);
    }

    /**
     * 递归查询某个菜单的所有子节点（包含所有层级）
     */
    private List<Long> getChildIdsRecursive(Long parentId) {
        List<Long> currentChildIds = lambdaQuery()
                .select(SysMenu::getId) // 只查ID
                .eq(SysMenu::getParentId, parentId)
                .list()
                .stream()
                .map(SysMenu::getId)
                .toList();

        // 加入当前层
        List<Long> childIds = new ArrayList<>(currentChildIds);

        // 递归查询子子孙孙
        for (Long childId : currentChildIds) {
            childIds.addAll(getChildIdsRecursive(childId));
        }

        return childIds;
    }

    /**
     * 菜单PO转换为VO对象
     */
    private MenuSimpleVO toMenuSimpleVO(SysMenu menu) {
        MenuSimpleVO vo = new MenuSimpleVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setName(menu.getName());
        vo.setPermission(menu.getPermission());
        vo.setType(menu.getType());
        vo.setSort(menu.getSort());
        vo.setPath(menu.getPath());
        vo.setIcon(menu.getIcon());
        vo.setComponent(menu.getComponent());
        vo.setCreateTime(menu.getCreateTime());
        vo.setStatus(menu.getStatus());
        return vo;
    }
}
