package com.changan.park.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.changan.common.constants.Constants;
import com.changan.common.enums.CommonStatus;
import com.changan.common.exceptions.BizIllegalException;
import com.changan.common.utils.StpKit;
import com.changan.common.utils.WechatUtil;
import com.changan.model.dto.AppLoginDTO;
import com.changan.model.dto.BindPhoneDTO;
import com.changan.model.dto.BindPhoneWxDTO;
import com.changan.model.dto.WxLoginDTO;
import com.changan.model.po.Customer;
import com.changan.model.vo.AppLoginVO;
import com.changan.park.mapper.CustomerCouponMapper;
import com.changan.park.mapper.CustomerMapper;
import com.changan.park.mapper.CustomerPlateMapper;
import com.changan.park.mapper.EntryExitRecordMapper;
import com.changan.park.mapper.ParkingOrderMapper;
import com.changan.park.service.IAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final CustomerMapper customerMapper;

    private final RedisTemplate<String, Object> redisTemplate;
    private final WechatUtil wechatUtil;
    private final ParkingOrderMapper parkingOrderMapper;
    private final CustomerPlateMapper customerPlateMapper;
    private final CustomerCouponMapper customerCouponMapper;
    private final EntryExitRecordMapper entryExitRecordMapper;

    @Override
    public AppLoginVO login(AppLoginDTO dto) {
        String phone = dto.getPhone();
        // 1.校验手机验证码
        verifySmsCode(phone, dto.getCode());
        // 2.按手机号查询客户
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getPhone, phone));

        boolean isNewUser = customer == null;
        if (isNewUser) {
            // 3.新用户按手机号来注册
            customer = registerByPhone(phone);
        }
        return doLogin(customer, isNewUser);
    }

    @Override
    public AppLoginVO wxLogin(WxLoginDTO dto) {
        // 1.拿code换openid
        String openid = wechatUtil.code2Session(dto.getCode());
        // 2.按openid查客户
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getOpenid, openid));
        boolean isNewUser = customer == null;
        if (isNewUser) {
            // 3.新用户按openid来注册
            customer = registerByOpenid(openid);
        }
        return doLogin(customer, isNewUser);
    }

    @Override
    public void bindPhone(BindPhoneDTO dto) {
        // 1.校验手机验证码
        verifySmsCode(dto.getPhone(), dto.getCode());
        // 2.绑定手机号
        doBindPhone(StpKit.APP.getLoginIdAsLong(), dto.getPhone());
    }

    @Override
    public String bindPhoneByWxCode(BindPhoneWxDTO dto) {
        String phone = wechatUtil.getPhoneNumber(dto.getWxCode());
        doBindPhone(StpKit.APP.getLoginIdAsLong(), phone);
        return phone;
    }

    @Override
    public void bindWx(WxLoginDTO dto) {
        // 1.获取当前登录用户ID
        Long customerId = StpKit.APP.getLoginIdAsLong();
        // 2.拿code 换 openid
        String openid = wechatUtil.code2Session(dto.getCode());
        // 3.写入账号
        doBindWx(customerId, openid);
    }

    @Override
    public void unbindWx() {
        Long customerId = StpKit.APP.getLoginIdAsLong();
        Customer current = customerMapper.selectById(customerId);
        if (current == null) {
            throw new BizIllegalException("账号不存在，请重新登录");
        }
        // 1.本来就没绑
        if (StrUtil.isBlank(current.getOpenid())) {
            throw new BizIllegalException("当前账号未绑定微信");
        }
        // 2.手机号是唯一剩下的登录方式，没绑手机就解绑微信，账号直接登不上了
        if (StrUtil.isBlank(current.getPhone())) {
            throw new BizIllegalException("请先绑定手机号，再解绑微信");
        }
        // 3.清空 openid
        customerMapper.update(null, new LambdaUpdateWrapper<Customer>()
                .eq(Customer::getId, customerId)
                .set(Customer::getOpenid, null));
    }

    /**
     * 把openid写到当前账号上
     */
    private void doBindWx(Long customerId, String openid) {
        Customer current = customerMapper.selectById(customerId);
        if (current == null) {
            throw new BizIllegalException("账号不存在，请重新登录");
        }
        // 1.已经绑的就是这个微信，幂等返回
        if (openid.equals(current.getOpenid())) {
            return;
        }
        // 2.这个微信已经被别的账号绑走了
        Customer exists = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getOpenid, openid));
        if (exists != null) {
            throw new BizIllegalException("该微信已关联其他账号，请更换微信或在原账号解绑");
        }
        // 3.绑定
        if (StrUtil.isNotBlank(current.getOpenid())) {
            log.info("[绑定微信] 换绑 customerId={}，旧openid={}", customerId, current.getOpenid());
        }
        current.setOpenid(openid);
        customerMapper.updateById(current);
    }

    /**
     * 把手机号写到当前登录账号上
     */
    private void doBindPhone(Long customerId, String phone) {
        Customer current = customerMapper.selectById(customerId);
        if (current == null) {
            throw new BizIllegalException("账号不存在，请重新登录");
        }
        // 1.已绑手机号
        if (StrUtil.isNotBlank(current.getPhone())) {
            if (phone.equals(current.getPhone())) {
                return;
            }
            throw new BizIllegalException("当前账号已绑定手机号，如需更换请联系客服");
        }
        // 2.手机号已被其他账号占用
        boolean exists = customerMapper.exists(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getPhone, phone));
        if (exists) {
            throw new BizIllegalException("该手机号已被其他账号绑定。若当前是未绑定手机号的微信临时账号，可先注销当前账号，再用该手机号登录并绑定微信；如无法注销请联系客服");
        }
        // 3.补上手机号
        current.setPhone(phone);
        customerMapper.updateById(current);
    }

    /**
     * 校验短信验证码：拉黑判断 + 错误计数 + 成功后一次性作废
     */
    private void verifySmsCode(String phone, String code) {
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
        if (!cacheCode.equals(code)) {
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
    }

    /**
     * 状态校验 + 登录 APP 账号空间 + 组装 VO
     */
    private AppLoginVO doLogin(Customer customer, boolean isNewUser) {
        if (customer.getStatus() != CommonStatus.ENABLE) {
            throw new BizIllegalException("账号已被禁用，如有疑问请联系客服");
        }
        StpKit.APP.login(customer.getId());
        AppLoginVO vo = BeanUtil.copyProperties(customer, AppLoginVO.class);
        vo.setToken(StpKit.APP.getTokenValue());
        vo.setCustomerId(customer.getId());
        vo.setIsNewUser(isNewUser);
        vo.setNeedBindPhone(StrUtil.isBlank(customer.getPhone()));
        return vo;
    }

    /**
     * 微信自动注册，昵称取“微信用户+4位随机数”
     */
    private Customer registerByOpenid(String openid) {
        Customer customer = new Customer();
        customer.setOpenid(openid);
        customer.setNickname("微信用户" + RandomUtil.randomNumbers(4));
        customer.setStatus(CommonStatus.ENABLE);
        try {
            customerMapper.insert(customer);
        } catch (Exception e) {
            log.warn("[微信注册]openid重复插入 openid={}", openid, e);
            Customer retry = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                    .eq(Customer::getOpenid, openid));
            if (retry == null) {
                throw new BizIllegalException("登录失败，请重试");
            }
            return retry;
        }
        return customer;
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
            log.warn("[自动注册]手机号重复插入 phone={}", phone, e);
            Customer retry = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                    .eq(Customer::getPhone, phone));
            if (retry == null) {
                throw new BizIllegalException("登录失败，请重试");
            }
            return retry;
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
