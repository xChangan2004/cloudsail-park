package com.changan.common.constants;

import java.time.format.DateTimeFormatter;

public interface Constants {

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
    }
}
