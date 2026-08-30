package com.changan.common.constants;

import java.time.Duration;
import java.time.format.DateTimeFormatter;

public interface Constants {

    interface Lot {
        /**
         * 停车场GEO索引key
         */
        String LOT_GEO_KEY = "parking:lot:geo:";
    }

    interface Spaces {
        /**
         * 余位缓存key
         */
        String FREE_COUNT_KEY = "parking:free:";
        /**
         * 缓存有效期（60秒）
         */
        long FREE_COUNT_TTL = 60;
    }

    interface Order {
        /**
         * 订单号时间部分格式
         */
        DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    }

    interface Dashboard {
        /**
         * 数据看板缓存key
         */
        String DASHBOARD_KEY = "dashboard:";
        /**
         * 日期格式
         */
        String DATE_FMT = "yyyy-MM-dd";
    }

    interface Admin {
        /**
         * 账户体系类型
         */
        String TYPE = "admin";
    }

    interface App {
        /**
         * 账户体系类型
         */
        String TYPE = "app";
        /**
         * 验证码缓存key前缀
         */
        String CODE_KEY_PREFIX = "app:sms:code:";
        /**
         * 发送限频key前缀
         */
        String LIMIT_KEY_PREFIX = "app:sms:limit:";
        /**
         * 验证码有效期（分钟）
         */
        Duration CODE_TTL = Duration.ofMinutes(5);
        /**
         * 发送间隔限制（秒）
         */
        Duration SEND_INTERVAL = Duration.ofSeconds(60);
        /**
         * 验证码错误key前缀
         */
        String FAIL_COUNT_KEY_PREFIX = "app:sms:fail:count:";
        /**
         * 拉黑锁定key前缀
         */
        String FAIL_BLOCK_KEY_PREFIX = "app:sms:fail:block:";
        /**
         * 最大验证码错误次数
         */
        int MAX_FAIL_TIMES = 5;
        /**
         * 锁定时长
         */
        Duration BLOCK_MINUTES = Duration.ofMinutes(10);
    }

    interface Coupon {
        /**
         * 领取优惠券锁key
         */
        String COUPON_RECEIVE_LOCK_KEY = "coupon:receive:#{customerId}:#{templateId}";
    }

    interface Pay {
        /**
         * 创建支付锁key
         */
        String CREATE_PAY_LOCK = "pay:create:#{orderId}";
        /**
         * 退款锁key
         */
        String REFUND_LOCK =  "pay:refund:#{dto.orderNo}";
    }
}
