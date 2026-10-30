package com.changan.common.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class RateLimitUtil {

    private final RScript script;

    public RateLimitUtil(RedissonClient redissonClient) {
        this.script = redissonClient.getScript(StringCodec.INSTANCE);
    }

    private static final String FIXED_WINDOW_LUA = """
            local key = KEYS[1]
            local expire = tonumber(ARGV[1])
            local count = redis.call('INCR', key)
            if count == 1 then
                redis.call('PEXPIRE', key, expire)
            end
            return count
            """;

    /**
     * 固定窗口计数限流。
     *
     * @param key    限流key
     * @param limit  窗口内允许的最大次数
     * @param window 窗口长度
     * @return true=放行，false=超限
     */
    public boolean tryAcquire(String key, int limit, Duration window) {
        Long count;
        try {
            count = script.eval(
                    RScript.Mode.READ_WRITE,
                    FIXED_WINDOW_LUA,
                    RScript.ReturnType.INTEGER,
                    List.of(key),
                    window.toMillis()
            );
        } catch (Exception e) {
            // 放行并留日志
            log.error("[限流] Redis 执行失败，本次放行 key={}", key, e);
            return true;
        }
        if (count == null) {
            log.warn("[限流] 脚本返回null，本次放行 key={}", key);
            return true;
        }
        if (count > limit) {
            log.debug("[限流] 触发限流，key={}, limit={}, current={}", key, limit, count);
            return false;
        }
        return true;
    }
}
