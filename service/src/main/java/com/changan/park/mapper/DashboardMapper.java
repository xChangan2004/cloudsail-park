package com.changan.park.mapper;

import com.changan.model.vo.DashboardCardsVO;
import com.changan.model.vo.DashboardTrendVO;
import com.changan.model.vo.OrderDistributionVO;
import com.changan.model.vo.PayMethodStatVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DashboardMapper {

    BigDecimal selectTodayIncome(
            @Param("begin") LocalDateTime begin,
            @Param("end") LocalDateTime end,
            @Param("lotId") Long lotId);

    DashboardCardsVO selectTodayEntryExit(
            @Param("begin") LocalDateTime begin,
            @Param("end") LocalDateTime end,
            @Param("lotId") Long lotId);


    DashboardCardsVO selectSpaceStat(@Param("lotId") Long lotId);

    List<DashboardTrendVO> selectIncomeTrend(
            @Param("begin") LocalDateTime begin,
            @Param("lotId") Long lotId);

    List<DashboardTrendVO> selectEntryTrend(
            @Param("begin") LocalDateTime begin,
            @Param("lotId") Long lotId);

    List<DashboardTrendVO> selectExitTrend(
            @Param("begin") LocalDateTime begin,
            @Param("lotId") Long lotId);

    List<OrderDistributionVO> selectOrderDistribution(@Param("lotId") Long lotId);

    List<PayMethodStatVO.MethodItem> selectPayMethodStat(@Param("lotId") Long lotId);

    BigDecimal selectTotalRefund(@Param("lotId") Long lotId);
}
