package com.changan.park.strategy.fee;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.changan.common.enums.FeeRuleType;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.model.dto.FeeRuleFormDTO;
import com.changan.model.po.FeeRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 分时段计费策略：按分钟比例计费，支持跨天时段（22:00‑08:00）
 */
@Component
public class PeriodFeeStrategy implements FeeCalculateStrategy {

    @Override
    public FeeRuleType supportType() {
        return FeeRuleType.PERIOD;
    }

    @Override
    public void validate(FeeRuleFormDTO dto) {
        if (CollUtil.isEmpty(dto.getTimeSegments())) {
            throw new BizIllegalException("分时段计费必须配置时段费率");
        }
        for (FeeRule.TimeSegment seg : dto.getTimeSegments()) {
            if (StrUtil.hasBlank(seg.getStart(), seg.getEnd()) || seg.getRate() == null) {
                throw new BizIllegalException("时段的起止时间和费率均不能为空");
            }
            try {
                LocalTime.parse(seg.getStart());
                LocalTime.parse(seg.getEnd());
            } catch (Exception e) {
                throw new BizIllegalException("时段时间格式错误，请使用HH:mm格式");
            }
        }
        validateNoOverlap(dto.getTimeSegments());
    }

    @Override
    public BigDecimal calculate(FeeRule rule, LocalDateTime entryTime, LocalDateTime exitTime) {
        long totalMinutes = Duration.between(entryTime, exitTime).toMinutes();
        // 1.免费时长内
        if (totalMinutes <= rule.getFreeMinutes()) {
            return BigDecimal.ZERO;
        }
        // 2.从免费时长结束的时刻开始，逐时段推进累加
        LocalDateTime cursor = entryTime.plusMinutes(rule.getFreeMinutes());
        BigDecimal fee = BigDecimal.ZERO;
        // 游标没到出场时间，就循环切片计算每一段费用
        while (cursor.isBefore(exitTime)) {
            // 根据当前cursor时刻，找到属于哪个收费时段（支持跨天时段）
            FeeRule.TimeSegment seg = findSegment(rule.getTimeSegments(), cursor);
            // 算出该时段在时间轴上真实结束时刻（自动处理跨天+1天）
            LocalDateTime segEnd = segmentEndTime(cursor, seg);
            // 本段计费截止时间 = 时段结束 与 出场时间 的较早者
            LocalDateTime chargeEnd = segEnd.isBefore(exitTime) ? segEnd : exitTime;
            long minutes = Duration.between(cursor, chargeEnd).toMinutes();
            // 不足1小时按比例折算（按分钟计费）
            fee = fee.add(seg.getRate()
                    .multiply(BigDecimal.valueOf(minutes))
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
            // 游标跳到本段末尾，继续处理下一段
            cursor = chargeEnd;
        }
        // 3.执行单日封顶逻辑：min(计算出来总费用，单日封顶 × 向上取整停车天数)
        return applyDailyCap(fee, rule, totalMinutes);
    }

    @Override
    public String getRuleDesc(FeeRule rule) {
        if (CollUtil.isEmpty(rule.getTimeSegments())) {
            return "分时段计费";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("免费").append(rule.getFreeMinutes()).append("分钟，");
        List<FeeRule.TimeSegment> segments = new ArrayList<>(rule.getTimeSegments());
        segments.sort(Comparator.comparing(FeeRule.TimeSegment::getStart));
        for (int i = 0; i < segments.size(); i++) {
            FeeRule.TimeSegment seg = segments.get(i);
            if (i > 0) {
                sb.append("，");
            }
            sb.append(seg.getStart()).append("-").append(seg.getEnd())
                    .append("每小时").append(seg.getRate()).append("元");
        }
        if (hasDailyCap(rule)) {
            sb.append("，单日封顶").append(rule.getDailyCap()).append("元");
        }
        return sb.toString();
    }

    /**
     * 校验时段之间不能有重叠（排序后相邻比较，允许有空隙，空隙时间不计费）
     *
     * @param segments 时段
     */
    private void validateNoOverlap(List<FeeRule.TimeSegment> segments) {
        List<int[]> list = segments.stream()
                .map(seg -> new int[]{toMinutes(seg.getStart()), toMinutes(seg.getEnd())})
                .sorted(Comparator.comparingInt(a -> a[0]))
                .toList();
        for (int i = 0; i < list.size() - 1; i++) {
            int[] cur = list.get(i);
            int[] next = list.get(i + 1);
            int curEnd = cur[1] <= cur[0] ? cur[1] + 1440 : cur[1]; // 跨天时段结束时间+1天
            if (curEnd > next[0]) {
                throw new BizIllegalException("时段配置存在重叠，请检查起止时间");
            }
        }
    }

    /**
     * 找到指定时间所属的时段（支持跨天时段，如 20:00-08:00）
     */
    private FeeRule.TimeSegment findSegment(List<FeeRule.TimeSegment> segments, LocalDateTime time) {
        // 把当前时间，换算成当天从0点开始的总分钟数。例：08:30 → 8*60+30 = 510 分钟
        int minute = time.getHour() * 60 + time.getMinute();
        return segments.stream()
                .filter(seg -> {
                    int s = toMinutes(seg.getStart()); // 时段开始时间转当天分钟，如"22:00" → 1320
                    int e = toMinutes(seg.getEnd()); // 时段结束时间转当天分钟，如"08:00" →480
                    if (s > e) { // 跨天时段
                        // 跨天场景：22:00 ~ 次日08:00
                        // 满足：(当前时间 >=22:00)  或者 (当前时间 <08:00)
                        return minute >= s || minute < e;
                    }
                    // 不跨天场景：08:00‑22:00
                    // 当前时间大于等于开始，小于结束
                    return minute >= s && minute < e;
                })
                .findFirst()
                .orElseThrow(() -> new BizIllegalException("时段配置未覆盖停车时间"));
    }

    /**
     * 计算时段的结束时间点（处理跨天时段）
     */
    private LocalDateTime segmentEndTime(LocalDateTime cursor, FeeRule.TimeSegment seg) {
        LocalDateTime segEnd = cursor.toLocalDate().atTime(LocalTime.parse(seg.getEnd()));
        // 结束时间不晚于当前时刻，说明时段跨天，结束时间在次日
        if (!segEnd.isAfter(cursor)) {
            segEnd = segEnd.plusDays(1);
        }
        return segEnd;
    }

    private int toMinutes(String time) {
        String[] arr = time.split(":");
        return Integer.parseInt(arr[0]) * 60 + Integer.parseInt(arr[1]);
    }
}
