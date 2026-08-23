package com.changan.common.config.redisson.enums;

import com.changan.common.config.redisson.annotations.Lock;
import com.changan.common.exceptions.BizIllegalException;
import org.redisson.api.RLock;

public enum LockStrategy {
    /**
     * 不重试，直接结束，返回false
     */
    SKIP_FAST {
        @Override
        public boolean tryLock(RLock lock, Lock prop) throws InterruptedException {
            return lock.tryLock(0, prop.leaseTime(), prop.timeUnit());
        }
    },
    /**
     * 不重试，直接结束，抛出异常
     */
    FAIL_FAST {
        @Override
        public boolean tryLock(RLock lock, Lock prop) throws InterruptedException {
            boolean success = lock.tryLock(0, prop.leaseTime(), prop.timeUnit());
            if (!success) {
                throw new BizIllegalException("请求太频繁");
            }
            return true;
        }
    },
    /**
     * 重试，请求超时后，直接结束
     */
    SKIP_AFTER_RETRY_TIMEOUT {
        @Override
        public boolean tryLock(RLock lock, Lock prop) throws InterruptedException {
            return lock.tryLock(prop.waitTime(), prop.leaseTime(), prop.timeUnit());
        }
    },
    /**
     * 重试，直到超时后，抛出异常
     */
    FAIL_AFTER_RETRY_TIMEOUT {
        @Override
        public boolean tryLock(RLock lock, Lock prop) throws InterruptedException {
            boolean success = lock.tryLock(prop.waitTime(), prop.leaseTime(), prop.timeUnit());
            if (!success) {
                throw new BizIllegalException("请求超时");
            }
            return true;
        }
    },
    /**
     * 不停重试，直到成功为止
     */
    KEEP_RETRY {
        @Override
        public boolean tryLock(RLock lock, Lock prop) throws InterruptedException {
            lock.lock(prop.leaseTime(), prop.timeUnit());
            return true;
        }
    };

    public abstract boolean tryLock(RLock lock, Lock prop) throws InterruptedException;
}
