package com.changan.park.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changan.common.constants.Constants;
import com.changan.common.utils.StpKit;
import com.changan.park.mapper.*;
import com.changan.park.service.ISysUserService;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.CommonStatus;
import com.changan.common.enums.MenuType;
import com.changan.common.exceptions.BadRequestException;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.common.utils.PasswordUtils;
import com.changan.common.utils.TreeUtils;
import com.changan.model.dto.UserFormDTO;
import com.changan.model.dto.UserLoginDTO;
import com.changan.model.dto.UserResetPwdDTO;
import com.changan.model.po.*;
import com.changan.model.query.SysUserQuery;
import com.changan.model.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static com.changan.common.config.SaPermissionConfig.EXTRA_PERM_LIST;
import static com.changan.common.config.SaPermissionConfig.EXTRA_ROLE_LIST;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;
    private final Executor bizTaskExecutor;
    private final HttpServletRequest request;

    /**
     * 用户登录认证
     *
     * @param loginDTO 登录参数（用户名、密码）
     * @return 登录结果（Token、用户信息、角色、权限、菜单树）
     */
    @Override
    public LoginVO login(UserLoginDTO loginDTO) {
        // 根据用户名查询用户信息
        SysUser user = lambdaQuery()
                .eq(SysUser::getUsername, loginDTO.getUsername())
                .one();

        // 校验用户状态、密码合法性
        validUser(user, loginDTO.getPassword());

        // 查询用户关联的角色ID列表
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                Wrappers.lambdaQuery(SysUserRole.class)
                        .eq(SysUserRole::getUserId, user.getId())
        );
        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());

        // 用户未分配任何角色，不允许登录
        if (roleIds.isEmpty()) {
            throw new BadRequestException("暂未分配角色，无法登录");
        }

        // 查询启用状态的角色信息
        List<SysRole> roles = roleMapper.selectList(
                Wrappers.lambdaQuery(SysRole.class)
                        .in(SysRole::getId, roleIds)
                        .eq(SysRole::getStatus, CommonStatus.ENABLE)
        );
        List<String> roleList = roles.stream()
                .map(SysRole::getCode)
                .collect(Collectors.toList());

        // 超级管理员特判：不受菜单绑定限制，登录时直接加载全部启用菜单
        boolean isSuperAdmin = roleList.contains(Constants.Admin.SUPER_ADMIN_CODE);

        // 查询角色关联的菜单ID
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(
                Wrappers.lambdaQuery(SysRoleMenu.class)
                        .in(SysRoleMenu::getRoleId, roleIds)
        );
        Set<Long> menuIds = roleMenus.stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toSet());

        // 普通角色未分配任何菜单，返回空权限（超管跳过，走全量加载）
        if (menuIds.isEmpty() && !isSuperAdmin) {
            StpKit.ADMIN.login(user.getId(), 28800);
            StpKit.ADMIN.getTokenSession().set(EXTRA_ROLE_LIST, roleList);
            StpKit.ADMIN.getTokenSession().set(EXTRA_PERM_LIST, Collections.emptyList());
            return buildLoginVO(user, roleList, Collections.emptyList(), Collections.emptyList());
        }

        // 超管加载全部启用菜单；普通角色按角色-菜单关联加载
        List<SysMenu> allMenus = menuMapper.selectList(
                Wrappers.lambdaQuery(SysMenu.class)
                        .in(!isSuperAdmin, SysMenu::getId, menuIds)
                        .eq(SysMenu::getStatus, CommonStatus.ENABLE)
        );

        // 提取权限标识（仅保留非空权限并去重）
        List<String> permList = allMenus.stream()
                .map(SysMenu::getPermission)
                .filter(p -> p != null && !p.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        // 执行登录，并将角色与权限存入当前Token专属会话
        StpKit.ADMIN.login(user.getId(), 28800);
        StpKit.ADMIN.getTokenSession().set(EXTRA_ROLE_LIST, roleList);
        StpKit.ADMIN.getTokenSession().set(EXTRA_PERM_LIST, permList);

        // 过滤出目录与菜单（排除按钮），构建前端路由菜单树
        List<MenuVO> navMenus = allMenus.stream()
                .filter(m -> m.getType() != MenuType.BUTTON)
                .map(this::toMenuVO)
                .toList();
        List<MenuVO> menuTree = TreeUtils
                .buildTree(navMenus, MenuVO::getId, MenuVO::getParentId, MenuVO::setChildren);

        // 异步调整登录IP和时间
        String loginIp = JakartaServletUtil.getClientIP(request);
        asyncUpdateLoginInfo(user.getId(), loginIp);

        // 构造登录返回结果
        return buildLoginVO(user, roleList, permList, menuTree);
    }
    @Override
    public void logout() {
        StpKit.ADMIN.logout();
    }

    @Override
    public UserDetailVO queryUserById(Long id) {
        // 1.查询用户
        SysUser user = getById(id);
        if (user == null) {
            throw new BadRequestException("用户不存在");
        }
        // 2.拷贝数据到VO
        UserDetailVO detailVO = BeanUtil.copyProperties(user, UserDetailVO.class);
        // 3.查询用户关联角色列表
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, user.getId()))
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();
        // 4.填充关联角色id
        detailVO.setRoleIds(roleIds);
        return detailVO;
    }

    @Override
    public PageDTO<UserPageVO> queryUserPage(SysUserQuery query) {
        // 1.分页查询
        Page<SysUser> page = lambdaQuery()
                .like(StrUtil.isNotBlank(query.getUsername()), SysUser::getUsername, query.getUsername())
                .eq(StrUtil.isNotBlank(query.getPhone()), SysUser::getPhone, query.getPhone())
                .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<SysUser> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 2.转换为VO
        List<UserPageVO> list = BeanUtil.copyToList(records, UserPageVO.class);
        // 3.返回数据
        return PageDTO.of(page, list);
    }

    @Override
    public void updateUserStatus(Long id, CommonStatus status) {
        // 1.查询用户
        SysUser exists = getById(id);
        if (exists == null) {
            // 不存在，抛出异常结束
            throw new BadRequestException("用户不存在");
        }
        // 2.调整用户状态
        if (exists.getStatus() == status) {
            // 避免无效调整
            return;
        }
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        updateById(user);
        // 3.判断是否为禁止状态，是则需要强退用户
        if (status == CommonStatus.DISABLE) {
            StpKit.ADMIN.kickout(id);
        }
    }

    @Override
    public void resetPwd(UserResetPwdDTO dto) {
        Long userId = dto.getUserId();
        String password = dto.getPassword();

        // 查询用户
        SysUser exists = getById(userId);
        if (exists == null) {
            throw new BadRequestException("用户不存在");
        }
        // 禁止新密码与旧密码相同
        if (PasswordUtils.matches(password, exists.getPassword())) {
            throw new BizIllegalException("新密码不能与原密码一致");
        }

        // 加密更新密码
        String encryptPwd = PasswordUtils.encode(password);
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(encryptPwd);
        updateById(update);

        // 强制下线所有会话
        StpKit.ADMIN.kickout(userId);
    }

    @Override
    @Transactional
    public void saveUser(UserFormDTO dto) {
        // 1.校验用户名唯一
        Long count = lambdaQuery()
                .eq(SysUser::getUsername, dto.getUsername())
                .count();
        if (count > 0) {
            throw new BizIllegalException("用户名已存在，不可重复创建");
        }
        // 2.拷贝用户数据
        SysUser user = BeanUtil.copyProperties(dto, SysUser.class);
        // 3.处理密码加密
        user.setPassword(PasswordUtils.encode(user.getPassword()));
        // 4.添加用户
        save(user);
        Long userId = user.getId();
        // 5.批量插入用户-角色关联关系
        List<SysUserRole> roleList = dto.getRoleIds().stream()
                .map(roleId -> {
                    SysUserRole ur = new SysUserRole();
                    ur.setUserId(userId);
                    ur.setRoleId(roleId);
                    return ur;
                }).collect(Collectors.toList());
        userRoleMapper.insert(roleList);
    }

    @Override
    @Transactional
    public void deleteUserById(Long id) {
        // 1.查询用户
        SysUser user = getById(id);
        if (user == null) {
            throw new BadRequestException("用户不存在");
        }
        // 2.当状态禁用时才可删除
        if (user.getStatus() == CommonStatus.ENABLE) {
            throw new BizIllegalException("所处状态不可删除");
        }
        // 3.删除用户和角色的关联
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, id));
        // 4.删除用户
        removeById(id);
        // 5.预防万一，需要踢出用户
        StpKit.ADMIN.kickout(id);
    }

    @Override
    @Transactional
    public void updateUser(UserFormDTO dto) {
        // 1.校验用户名唯一，去除自己
        Long count = lambdaQuery()
                .eq(SysUser::getUsername, dto.getUsername())
                .ne(SysUser::getId, dto.getId())
                .count();
        if (count > 0) {
            throw new BizIllegalException("用户名已存在，不可重复修改");
        }
        // 2.拷贝用户数据
        SysUser user = BeanUtil.copyProperties(dto, SysUser.class);
        // 3.处理密码加密
        if (StrUtil.isNotBlank(dto.getPassword())) {
            user.setPassword(PasswordUtils.encode(dto.getPassword()));
        } else {
            user.setPassword(null);  // 不更新密码字段
        }
        // 4.修改用户
        updateById(user);
        // 4.1 如果状态变更为禁用，踢出在线用户
        if (dto.getStatus() != null && dto.getStatus() == CommonStatus.DISABLE) {
            // 查询原状态
            SysUser original = getById(dto.getId());
            if (original.getStatus() == CommonStatus.ENABLE) {
                StpKit.ADMIN.kickout(dto.getId());
            }
        }
        // 5.移除原用户和角色的关联
        userRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, user.getId())
        );
        // 6.重新建立新的关联
        List<SysUserRole> roleList = dto.getRoleIds().stream()
                .map(roleId -> {
                    SysUserRole ur = new SysUserRole();
                    ur.setUserId(user.getId());
                    ur.setRoleId(roleId);
                    return ur;
                }).collect(Collectors.toList());
        userRoleMapper.insert(roleList);
    }

    /**
     * 异步修改用户的登录信息
     */
    private void asyncUpdateLoginInfo(Long userId, String loginIp) {
        CompletableFuture.runAsync(() -> {
            lambdaUpdate()
                    .set(SysUser::getLoginIp, loginIp)
                    .set(SysUser::getLoginDate, LocalDateTime.now())
                    .eq(SysUser::getId, userId)
                    .update();
        }, bizTaskExecutor);
    }

    /**
     * 校验用户合法性：是否存在、密码是否正确、状态是否正常
     */
    private void validUser(SysUser user, String password) {
        if (user == null) {
            throw new BadRequestException("用户不存在");
        }
        if (!PasswordUtils.matches(password, user.getPassword())) {
            throw new BadRequestException("密码错误");
        }
        if (user.getStatus() == CommonStatus.DISABLE) {
            throw new BadRequestException("账号已被禁用，禁止访问");
        }
    }

    /**
     * 菜单PO转换为VO对象
     */
    private MenuVO toMenuVO(SysMenu menu) {
        MenuVO vo = new MenuVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setName(menu.getName());
        vo.setType(menu.getType());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setIcon(menu.getIcon());
        vo.setSort(menu.getSort());
        return vo;
    }

    /**
     * 构建登录返回VO
     * 封装Token、用户信息、角色、权限、菜单树
     */
    private LoginVO buildLoginVO(SysUser user, List<String> roles,
                                 List<String> perms, List<MenuVO> menus) {
        UserInfoVO info = new UserInfoVO();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setNickname(user.getNickname());
        info.setAvatar(user.getAvatar());
        info.setPhone(user.getPhone());

        LoginVO vo = new LoginVO();
        vo.setToken(StpKit.ADMIN.getTokenValue());
        vo.setUserInfo(info);
        vo.setRoleList(roles);
        vo.setPermList(perms);
        vo.setMenus(menus);
        return vo;
    }
}