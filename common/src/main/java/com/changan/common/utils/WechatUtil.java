package com.changan.common.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.changan.common.config.wechat.WechatProperties;
import com.changan.common.constants.Constants;
import com.changan.common.exceptions.BizIllegalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class WechatUtil {

    private final WechatProperties wechatProperties;

    private final RestClient wechatRestClient;

    private final RedisTemplate<String, Object> redisTemplate;

    private final RedissonClient redissonClient;

    /**
     * code 换取 openid
     */
    public String code2Session(String jsCode) {
        JSONObject resp;
        try {
            resp = parse(getForJson(wechatProperties.getJscode2sessionUrl()
                            + "?appid={appid}&secret={secret}&js_code={jsCode}&grant_type=authorization_code",
                    wechatProperties.getAppid(),
                    wechatProperties.getAppSecret(),
                    jsCode));
        } catch (Exception e) {
            log.error("[微信登录] 调用 jscode2session 异常: {}", e.getClass().getSimpleName());
            throw new BizIllegalException("微信登录失败，请稍后重试");
        }

        if (resp == null) {
            throw new BizIllegalException("微信登录失败，请稍后重试");
        }
        Integer errcode = resp.getInt("errcode");
        if (errcode != null && errcode != 0) {
            log.warn("[微信登录] jscode2session 返回错误 errcode={}, errmsg={}",
                    errcode, resp.getStr("errmsg"));
            if (errcode == 40029) {
                throw new BizIllegalException("登录凭证已失效，请重新登录");
            }
            throw new BizIllegalException("微信登录失败，请稍后重试");
        }
        String openid = (String) resp.get("openid");
        if (StrUtil.isBlank(openid)) {
            log.error("[微信登录] jscode2session 未返回 openid，原始响应: {}", resp);
            throw new BizIllegalException("微信登录失败，请稍后重试");
        }
        return openid;
    }

    /**
     * 获取接口调用凭证
     */
    public String getAccessToken() {
        String cached = readCachedAccessToken();
        if (cached != null) {
            return cached;
        }

        RLock lock = redissonClient.getLock(Constants.App.WX_ACCESS_TOKEN_LOCK);
        boolean locked = false;
        try {
            // 只等 3 秒：抢不到说明别的线程/实例正在刷，直接读它刷好的结果
            locked = lock.tryLock(3, 30, TimeUnit.SECONDS);
            if (!locked) {
                String retry = readCachedAccessToken();
                if (retry != null) {
                    return retry;
                }
                throw new BizIllegalException("微信服务繁忙，请稍后重试");
            }
            // 等锁期间可能已经被刷完了，二次检查
            String afterLock = readCachedAccessToken();
            if (afterLock != null) {
                return afterLock;
            }
            return refreshAccessToken();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizIllegalException("微信服务繁忙，请稍后重试");
        } finally {
            // 只释放自己持有的锁
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 小程序手机号快速验证
     */
    public String getPhoneNumber(String code) {
        String accessToken = getAccessToken();
        JSONObject resp;
        try {
            resp = parse(postJson(
                    wechatProperties.getPhoneNumberUrl() + "?access_token={accessToken}",
                    Map.of("code", code),
                    accessToken));
        } catch (Exception e) {
            log.error("[微信手机号] 调用 getuserphonenumber 异常: {}", e.getMessage(), e);
            throw new BizIllegalException("获取微信手机号失败，请改用短信验证码绑定");
        }

        Integer errcode = resp == null ? null : resp.getInt("errcode");
        if (resCodeInvalid(errcode)) {
            // access_token 失效应立即清缓存，下次自动重新拉取
            log.warn("[微信手机号] access_token 失效，清理缓存 errcode={}", errcode);
            redisTemplate.delete(Constants.App.WX_ACCESS_TOKEN_KEY);
            throw new BizIllegalException("微信登录状态已失效，请重新登录");
        }
        if (errcode != null && errcode != 0) {
            log.warn("[微信手机号] 获取手机号失败 errcode={}, errmsg={}",
                    errcode, resp.getStr("errmsg"));
            throw new BizIllegalException("获取微信手机号失败，请改用短信验证码绑定");
        }

        JSONObject phoneInfo = resp.getJSONObject("phone_info");
        String phone = phoneInfo == null ? null :
                phoneInfo.getStr("purePhoneNumber");
        if (StrUtil.isBlank(phone)) {
            log.error("[微信手机号] 返回结果缺少手机号，原始响应: {}", resp);
            throw new BizIllegalException("获取微信手机号失败，请改用短信验证码绑定");
        }
        return phone;
    }

    /**
     * 读取缓存的 access_token，空值返回 null
     */
    private String readCachedAccessToken() {
        Object cached = redisTemplate.opsForValue().get(Constants.App.WX_ACCESS_TOKEN_KEY);
        if (cached == null || StrUtil.isBlank(cached.toString())) {
            return null;
        }
        return cached.toString();
    }

    /**
     * 调微信接口重新拉取 access_token 并写缓存
     */
    private String refreshAccessToken() {
        JSONObject resp;
        try {
            resp = parse(getForJson(wechatProperties.getAccessTokenUrl()
                            + "?grant_type=client_credential&appid={appid}&secret={secret}",
                    wechatProperties.getAppid(),
                    wechatProperties.getAppSecret()));
        } catch (Exception e) {
            log.error("[微信] 获取 access_token 异常: {}", e.getMessage(), e);
            throw new BizIllegalException("微信服务暂时不可用，请稍后重试");
        }

        String accessToken = resp == null ? null : resp.getStr("access_token");
        if (StrUtil.isBlank(accessToken)) {
            log.error("[微信] 获取 access_token 失败，原始响应: {}", resp);
            throw new BizIllegalException("微信服务异常，请稍后重试");
        }

        Long expiresIn = resp.getLong("expires_in");
        long seconds = expiresIn == null ? 7200L : expiresIn;
        // 提前 5 分钟过期，避免边界时刻拿到刚失效的 token
        long ttl = Math.max(seconds - 300, 60);
        redisTemplate.opsForValue()
                .set(Constants.App.WX_ACCESS_TOKEN_KEY, accessToken, Duration.ofSeconds(ttl));
        log.info("[微信] access_token 已刷新，缓存 {} 秒", ttl);
        return accessToken;
    }

    /**
     * 40001/40014/42001 均为 access_token 无效或过期
     */
    private boolean resCodeInvalid(Number errcode) {
        if (errcode == null) {
            return false;
        }
        int code = errcode.intValue();
        return code == 40001 || code == 40014 || code == 42001;
    }

    /**
     * GET 微信接口，取原始字节自己解码
     * <p>
     * 微信 jscode2session / cgi-bin/token 返回的 Content-Type 是 text/plain，
     * 直接 .body(Map.class) 会抛 UnknownContentTypeException；
     * 取 byte[] 则不参与内容协商，且不受未声明 charset 时按 ISO-8859-1 解码的影响。
     */
    private byte[] getForJson(String uriTemplate, Object... uriVars) {
        return wechatRestClient.get()
                .uri(uriTemplate, uriVars)
                .retrieve()
                .body(byte[].class);
    }

    /**
     * POST JSON 到微信接口，取原始字节
     */
    private byte[] postJson(String uriTemplate, Object body, Object... uriVars) {
        return wechatRestClient.post()
                .uri(uriTemplate, uriVars)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(byte[].class);
    }

    /**
     * 解析
     */
    private JSONObject parse(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        String raw = new String(bytes, StandardCharsets.UTF_8);
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        return JSONUtil.parseObj(raw);
    }
}
