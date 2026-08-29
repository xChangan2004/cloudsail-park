package com.changan.park.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changan.park.mapper.SysOperateLogMapper;
import com.changan.park.mapper.SysUserMapper;
import com.changan.park.service.ISysOperateService;
import com.changan.common.domain.dto.PageDTO;
import com.changan.model.po.SysOperateLog;
import com.changan.model.po.SysUser;
import com.changan.model.query.SysOperateLogQuery;
import com.changan.model.vo.SysOperateLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysOperateServiceImpl extends ServiceImpl<SysOperateLogMapper, SysOperateLog> implements ISysOperateService {

    private final SysUserMapper sysUserMapper;

    @Async("bizTaskExecutor")
    @Override
    public void saveLog(SysOperateLog operateLog) {
        try {
            save(operateLog);
        } catch (Exception e) {
            // 日志入库失败只打错误日志，绝不影响业务
            log.error("操作日志入库失败 type={} subType={} url={}",
                    operateLog.getType(), operateLog.getSubType(), operateLog.getRequestUrl(), e);
        }
    }

    @Override
    public PageDTO<SysOperateLogVO> queryOperLogPage(SysOperateLogQuery query) {
        // 1.分页查询日志
        Page<SysOperateLog> page = lambdaQuery()
                .eq(query.getUserId() != null, SysOperateLog::getUserId, query.getUserId())
                .eq(StrUtil.isNotBlank(query.getType()), SysOperateLog::getType, query.getType())
                .like(StrUtil.isNotBlank(query.getSubType()), SysOperateLog::getSubType, query.getSubType())
                .ge(query.getBeginTime() != null, SysOperateLog::getCreateTime, query.getBeginTime())
                .le(query.getEndTime() != null, SysOperateLog::getCreateTime, query.getEndTime())
                .eq(query.getSuccess() != null, SysOperateLog::getSuccess, query.getSuccess())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<SysOperateLog> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 2.批量查询用户名
        Set<Long> userIds = records.stream()
                .filter(r -> r.getUserId() != null && r.getUserId() > 0)
                .map(SysOperateLog::getUserId)
                .collect(Collectors.toSet());
        Map<Long, String> usernameMap = userIds.isEmpty() ? Map.of() :
                sysUserMapper.selectByIds(userIds)
                        .stream()
                        .collect(Collectors.toMap(SysUser::getId, SysUser::getUsername));
        // 3.组装VO
        return PageDTO.of(page, record -> {
            SysOperateLogVO vo = BeanUtil.copyProperties(record, SysOperateLogVO.class);
            vo.setUsername(usernameMap.get(record.getUserId()));
            return vo;
        });
    }
}
