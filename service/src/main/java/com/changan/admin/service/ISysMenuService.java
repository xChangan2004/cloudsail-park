package com.changan.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.changan.model.po.SysMenu;
import com.changan.model.query.SysMenuQuery;
import com.changan.model.vo.MenuSimpleVO;

import java.util.List;

public interface ISysMenuService extends IService<SysMenu> {

    List<MenuSimpleVO> queryMenuList(SysMenuQuery query);

    void deleteMenuById(Long id);

    void saveMenu(SysMenu sysMenu);

    void updateMenu(SysMenu sysMenu);

    SysMenu queryMenuById(Long id);
}
