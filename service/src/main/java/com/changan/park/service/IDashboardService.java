package com.changan.park.service;

import com.changan.model.vo.*;

import java.util.List;

public interface IDashboardService {

    DashboardCardsVO queryCards(Long lotId);

    List<DashboardTrendVO> queryTrend(Long lotId);

    List<OrderDistributionVO> queryOrderDistribution(Long lotId);

    PayMethodStatVO queryPayMethodStat(Long lotId);

    DashboardPlatformVO queryPlatform(Integer days);
}
