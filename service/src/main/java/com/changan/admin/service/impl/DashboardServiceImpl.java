package com.changan.admin.service.impl;

import cn.hutool.json.JSONUtil;
import com.changan.admin.mapper.DashboardMapper;
import com.changan.admin.service.IDashboardService;
import com.changan.common.constants.Constants;
import com.changan.common.enums.OrderStatus;
import com.changan.common.enums.PayMethod;
import com.changan.model.vo.DashboardCardsVO;
import com.changan.model.vo.DashboardTrendVO;
import com.changan.model.vo.OrderDistributionVO;
import com.changan.model.vo.PayMethodStatVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

    private final DashboardMapper dashboardMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern(Constants.Dashboard.DATE_FMT);

    @Override
    public DashboardCardsVO queryCards(Long lotId) {
        return queryWithCache("cards:" + keySuffix(lotId),
                () -> loadCards(lotId),
                json -> JSONUtil.toBean(json, DashboardCardsVO.class));
    }

    @Override
    public List<DashboardTrendVO> queryTrend(Long lotId) {
        return queryWithCache("trend:" + keySuffix(lotId),
                () -> loadTrend(lotId),
                json -> JSONUtil.toList(json, DashboardTrendVO.class));
    }

    @Override
    public List<OrderDistributionVO> queryOrderDistribution(Long lotId) {
        return queryWithCache("distribution:" + keySuffix(lotId),
                () -> loadDistribution(lotId),
                json -> JSONUtil.toList(json, OrderDistributionVO.class));
    }

    @Override
    public PayMethodStatVO queryPayMethodStat(Long lotId) {
        return queryWithCache("pay-method:" + keySuffix(lotId),
                () -> loadPayMethodStat(lotId),
                json -> JSONUtil.toBean(json, PayMethodStatVO.class));
    }

    private DashboardCardsVO loadCards(Long lotId) {
        // 1.获取今日开始和结束时间
        LocalDateTime todayBegin = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowBegin = todayBegin.plusDays(1);
        // 2.查数据库
        BigDecimal income = dashboardMapper.selectTodayIncome(todayBegin, tomorrowBegin, lotId);
        DashboardCardsVO entryExit = dashboardMapper.selectTodayEntryExit(todayBegin, tomorrowBegin, lotId);
        DashboardCardsVO spaces = dashboardMapper.selectSpaceStat(lotId);
        // 3.填充数据
        DashboardCardsVO vo = new DashboardCardsVO();
        vo.setTodayIncome(income);
        vo.setTodayEntryCount(entryExit.getTodayEntryCount());
        vo.setTodayExitCount(entryExit.getTodayExitCount());
        vo.setTotalSpaces(spaces.getTotalSpaces());
        vo.setFreeSpaces(spaces.getFreeSpaces());
        vo.setOccupiedSpaces(spaces.getOccupiedSpaces());
        return vo;
    }

    private List<DashboardTrendVO> loadTrend(Long lotId) {
        LocalDateTime begin = LocalDate.now().minusDays(6).atStartOfDay();
        // 1.先创建7天骨架
        Map<String, DashboardTrendVO> skeleton = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            // 1.1.获取日期
            String date = LocalDate.now().minusDays(i)
                    .format(DATE_FMT);
            // 1.2.创建VO填充基础数据
            DashboardTrendVO vo = new DashboardTrendVO();
            vo.setDate(date);
            vo.setIncome(BigDecimal.ZERO);
            vo.setEntryCount(0L);
            vo.setExitCount(0L);
            // 1.3.添加到Map中
            skeleton.put(date, vo);
        }
        // 2.查询数据库，各自填充对应字段
        dashboardMapper.selectIncomeTrend(begin, lotId)
                .forEach(t -> mergeTrend(skeleton, t.getDate(), t.getIncome(), null, null));
        dashboardMapper.selectEntryTrend(begin, lotId)
                .forEach(t -> mergeTrend(skeleton, t.getDate(), null, t.getEntryCount(), null));
        dashboardMapper.selectExitTrend(begin, lotId)
                .forEach(t -> mergeTrend(skeleton, t.getDate(), null, null, t.getExitCount()));
        return new ArrayList<>(skeleton.values());
    }

    private void mergeTrend(Map<String, DashboardTrendVO> skeleton, String date, BigDecimal income, Long entryCount, Long exitCount) {
        DashboardTrendVO vo = skeleton.get(date);
        if (vo == null) {
            // 超出近7天范围的数据（理论不会出现），直接丢弃
            return;
        }
        if (income != null) {
            vo.setIncome(income);
        }
        if (entryCount != null) {
            vo.setEntryCount(entryCount);
        }
        if (exitCount != null) {
            vo.setExitCount(exitCount);
        }
    }

    private List<OrderDistributionVO> loadDistribution(Long lotId) {
        // 1.查库
        Map<OrderStatus, Long> countMap = dashboardMapper.selectOrderDistribution(lotId)
                .stream()
                .collect(Collectors.toMap(OrderDistributionVO::getStatus, OrderDistributionVO::getCount));
        // 2.按枚举补齐状态
        List<OrderDistributionVO> voList = new ArrayList<>();
        for (OrderStatus status : OrderStatus.values()) {
            OrderDistributionVO vo = new OrderDistributionVO();
            vo.setStatus(status);
            vo.setStatusDesc(status.getDesc());
            vo.setCount(countMap.getOrDefault(status, 0L));
            voList.add(vo);
        }
        return voList;
    }

    private PayMethodStatVO loadPayMethodStat(Long lotId) {
        // 1.缴费方式，按枚举补齐
        Map<PayMethod, PayMethodStatVO.MethodItem> itemMap = dashboardMapper.selectPayMethodStat(lotId)
                .stream()
                .collect(Collectors.toMap(PayMethodStatVO.MethodItem::getMethod, Function.identity()));
        List<PayMethodStatVO.MethodItem> methods = new ArrayList<>();
        for (PayMethod method : PayMethod.values()) {
            PayMethodStatVO.MethodItem item = itemMap.get(method);
            if (item == null) {
                item = new PayMethodStatVO.MethodItem();
                item.setMethod(method);
                item.setCount(0L);
                item.setAmount(BigDecimal.ZERO);
            }
            item.setMethodDesc(method.getDesc());
            methods.add(item);
        }
        // 2.退款总额
        PayMethodStatVO vo = new PayMethodStatVO();
        vo.setMethods(methods);
        vo.setTotalRefund(dashboardMapper.selectTotalRefund(lotId));
        return vo;
    }

    /**
     * 统一缓存读写：命中返回，未命中查库回填，Redis 异常降级直查
     * deserializer 参数负责把 JSON 字符串还原成目标类型（VO 或 List<VO>）
     */
    private <T> T queryWithCache(String keySuffix,
                                 Supplier<T> dbLoader,
                                 Function<String, T> deserializer) {
        // 1.构建key
        String key = Constants.Dashboard.DASHBOARD_KEY + keySuffix;
        // 2.读缓存
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return deserializer.apply(cached.toString());
            }
        } catch (Exception e) {
            log.warn("看板缓存读取失败，降级查库 key={}", key, e);
        }
        // 3.降级查库存
        T result = dbLoader.get();
        // 4.回填缓存
        try {
            // 4.1.生成45~60s的随机值，用于缓存的有效时间，避免同一时间缓存雪崩
            int actualTtl = ThreadLocalRandom.current().nextInt(45, 61);
            // 4.2.写入缓存
            redisTemplate.opsForValue()
                    .set(key, JSONUtil.toJsonStr(result), actualTtl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("看板缓存写入失败 key={}", key, e);
        }
        return result;
    }

    private String keySuffix(Long lotId) {
        return lotId == null ? "all" : lotId.toString();
    }
}
