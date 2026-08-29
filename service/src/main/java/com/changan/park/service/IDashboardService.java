package com.changan.park.service;

import com.changan.model.vo.DashboardCardsVO;
import com.changan.model.vo.DashboardTrendVO;
import com.changan.model.vo.OrderDistributionVO;
import com.changan.model.vo.PayMethodStatVO;

import java.util.List;

public interface IDashboardService {

    DashboardCardsVO queryCards(Long lotId);

    List<DashboardTrendVO> queryTrend(Long lotId);

    List<OrderDistributionVO> queryOrderDistribution(Long lotId);

    PayMethodStatVO queryPayMethodStat(Long lotId);
}
