package com.changan.common.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@SuppressWarnings("all")
@AutoConfiguration
public class SaPermissionConfig implements StpInterface {

    public final static String EXTRA_PERM_LIST = "permList";
    public final static String EXTRA_ROLE_LIST = "roleList";

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        try {
            Object obj = StpUtil.getTokenSession().get(EXTRA_PERM_LIST);
            if (obj instanceof List) {
                return new ArrayList<>((List<String>) obj);
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.info("获取权限列表失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        try {
            Object obj = StpUtil.getTokenSession().get(EXTRA_ROLE_LIST);
            if (obj instanceof List) {
                return new ArrayList<>((List<String>) obj);
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.info("获取角色列表失败", e);
            return new ArrayList<>();
        }
    }
}
