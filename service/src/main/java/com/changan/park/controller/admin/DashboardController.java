package com.changan.park.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.changan.common.constants.Constants;
import com.changan.park.service.IDashboardService;
import com.changan.common.config.operlog.OperLog;
import com.changan.common.domain.R;
import com.changan.model.vo.DashboardCardsVO;
import com.changan.model.vo.DashboardTrendVO;
import com.changan.model.vo.OrderDistributionVO;
import com.changan.model.vo.PayMethodStatVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "数据看板相关接口")
public class DashboardController {

    private final IDashboardService dashboardService;

    @GetMapping("/cards")
    @Operation(summary = "获取看板卡片汇总指标")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "dashboard:view")
    @OperLog(type = "数据看板", subType = "获取看板卡片汇总指标")
    public R<DashboardCardsVO> queryCards(@RequestParam(required = false) Long lotId) {
        return R.ok(dashboardService.queryCards(lotId));
    }

    @GetMapping("/trend")
    @Operation(summary = "近7天收入与车流趋势")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "dashboard:view")
    public R<List<DashboardTrendVO>> queryTrend(@RequestParam(required = false) Long lotId) {
        return R.ok(dashboardService.queryTrend(lotId));
    }

    @GetMapping("/distribution")
    @Operation(summary = "订单状态分布")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "dashboard:view")
    public R<List<OrderDistributionVO>> queryOrderDistribution(@RequestParam(required = false) Long lotId) {
        return R.ok(dashboardService.queryOrderDistribution(lotId));
    }

    @GetMapping("/pay-method")
    @Operation(summary = "支付方式统计")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "dashboard:view")
    public R<PayMethodStatVO> queryPayMethodStat(@RequestParam(required = false) Long lotId) {
        return R.ok(dashboardService.queryPayMethodStat(lotId));
    }
}
