package com.changan.common.constants;

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
}
