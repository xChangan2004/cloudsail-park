package com.changan.park.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.changan.common.constants.Constants;
import com.changan.common.enums.CommonStatus;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.common.utils.StpKit;
import com.changan.model.dto.AppLoginDTO;
import com.changan.model.po.Customer;
import com.changan.model.vo.AppLoginVO;
import com.changan.park.mapper.CustomerMapper;
import com.changan.park.service.IAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final CustomerMapper customerMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public AppLoginVO login(AppLoginDTO dto) {
        String phone = dto.getPhone();
        // 1.先检查是否被拉黑锁定
        Boolean isBlocked = redisTemplate.hasKey(Constants.App.FAIL_BLOCK_KEY_PREFIX + phone);
        if (Boolean.TRUE.equals(isBlocked)) {
            throw new BizIllegalException("验证码错误次数过多，请" + Constants.App.BLOCK_MINUTES.toMinutes() + "分钟后重试");
        }
        // 2.校验验证码
        String cacheCode = (String) redisTemplate.opsForValue()
                .get(Constants.App.CODE_KEY_PREFIX + phone);
        if (StrUtil.isBlank(cacheCode)) {
            throw new BizIllegalException("验证码已过期");
        }
        if (!cacheCode.equals(dto.getCode())) {
            // 验证码错误，错误次数+1，Redis incr原子自增
            long failTimes = redisTemplate.opsForValue()
                    .increment(Constants.App.FAIL_COUNT_KEY_PREFIX + phone);
            // 首次自增时设置过期时间与验证码对齐
            if (failTimes == 1) {
                redisTemplate.expire(Constants.App.FAIL_COUNT_KEY_PREFIX + phone,
                        Constants.App.CODE_TTL);
            }
            // 达到最大错误次数：拉黑锁定并作废验证码
            if (failTimes >= Constants.App.MAX_FAIL_TIMES) {
                redisTemplate.opsForValue()
                        .set(Constants.App.FAIL_BLOCK_KEY_PREFIX + phone, "1",
                                Constants.App.BLOCK_MINUTES);
                redisTemplate.delete(Constants.App.CODE_KEY_PREFIX + phone);
                redisTemplate.delete(Constants.App.FAIL_COUNT_KEY_PREFIX + phone);
                throw new BizIllegalException("验证码错误次数过多，请" + Constants.App.BLOCK_MINUTES.toMinutes() + "分钟后重试");
            }
            // 未达到上限，提示还剩余多少次机会
            int remain = Constants.App.MAX_FAIL_TIMES - (int) failTimes;
            throw new BizIllegalException("验证码错误，还剩余" + remain + "次尝试机会");
        }
        // 3.验证码正确
        redisTemplate.delete(Constants.App.FAIL_COUNT_KEY_PREFIX + phone);
        redisTemplate.delete(Constants.App.CODE_KEY_PREFIX + phone);
        // 4.按手机号查询用户
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getPhone, phone));
        // 5.不存在则自动注册
        boolean isNewUser = customer == null;
        if (isNewUser) {
            customer = registerByPhone(phone);
        }
        // 6.状态校验
        if (customer.getStatus() != CommonStatus.ENABLE) {
            throw new BizIllegalException("账号已被禁用，如有疑问请联系客服");
        }
        // 7.登录APP账号空间
        StpKit.APP.login(customer.getId());
        // 8.组装VO返回
        AppLoginVO vo = BeanUtil.copyProperties(customer, AppLoginVO.class);
        vo.setToken(StpKit.APP.getTokenValue());
        vo.setCustomerId(customer.getId());
        vo.setIsNewUser(isNewUser);
        return vo;
    }

    /**
     * 手机号自动注册，昵称取“用户+手机尾号”
     */
    private Customer registerByPhone(String phone) {
        Customer customer = new Customer();
        customer.setPhone(phone);
        customer.setNickname("用户" + StrUtil.sub(phone, 7, 11));
        customer.setStatus(CommonStatus.ENABLE);
        try {
            customerMapper.insert(customer);
        } catch (Exception e) {
            // 捕获唯一索引冲突，说明已经有这个用户，重新查询返回
            log.warn("[自动注册]手机号重复插入 phone={}", phone, e);
            return customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                    .eq(Customer::getPhone, phone));
        }
        return customer;
    }

    @Override
    public void logout() {
        StpKit.APP.logout();
    }

    @Override
    public void code(String phone) {
        // 1.限频，规定时间内只允许发送一条
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(Constants.App.LIMIT_KEY_PREFIX + phone, "1", Constants.App.SEND_INTERVAL);
        if (Boolean.FALSE.equals(first)) {
            throw new BizIllegalException("发送太频繁，请" + Constants.App.SEND_INTERVAL.toSeconds() + "秒后再试");
        }
        // 2.生成6位数字验证码，存入Redis并设置有效时间；存储失败则回滚限频key，避免用户白等冷却时间
        String code = RandomUtil.randomNumbers(6);
        try {
            redisTemplate.opsForValue()
                    .set(Constants.App.CODE_KEY_PREFIX + phone, code, Constants.App.CODE_TTL);
        } catch (Exception e) {
            log.error("[验证码存储] Redis异常 phone={}", phone, e);
            redisTemplate.delete(Constants.App.LIMIT_KEY_PREFIX + phone);
            throw new BizIllegalException("系统繁忙，请稍后重试");
        }
        // 3.模拟发送，打印到控制台
        log.info("[用户端登录验证码] phone={}, code={}", phone, code);
    }
}
