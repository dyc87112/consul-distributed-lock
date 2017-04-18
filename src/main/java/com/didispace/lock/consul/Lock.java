package com.didispace.lock.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.kv.model.PutParams;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 基于Consul的互斥锁
 *
 * @author 翟永超
 * @create 2017/4/6.
 * @blog http://blog.didispace.com
 */
@Slf4j
public class Lock extends BaseLock {

    private static final String prefix = "lock/";  // 同步锁参数前缀

    /**
     * @param consulClient
     * @param lockKey       同步锁在consul的KV存储中的Key路径，会自动增加prefix前缀，方便归类查询
     * @param checkTtl      对锁Session的TTL
     */
    public Lock(ConsulClient consulClient, String lockKey, CheckTtl checkTtl) {
        super(consulClient, prefix + lockKey, checkTtl);
    }

    /**
     * 获取同步锁
     *
     * @param block            是否阻塞，直到获取到锁为止，默认尝试间隔时间为500ms。
     * @return
     */
    public Boolean lock(boolean block) throws InterruptedException {
        return lock(block, 500L, null);
    }


    /**
     * 获取同步锁
     *
     * @param block            是否阻塞，直到获取到锁为止
     * @param timeInterval     block=true时有效，再次尝试的间隔时间
     * @param maxTimes         block=true时有效，最大尝试次数
     * @return
     */
    public Boolean lock(boolean block, Long timeInterval, Integer maxTimes) throws InterruptedException {
        if (sessionId != null) {
            throw new RuntimeException(sessionId + " - Already locked!");
        }
        sessionId = createSession("lock-" + this.keyPath);
        int count = 1;
        while(true) {
            PutParams putParams = new PutParams();
            putParams.setAcquireSession(sessionId);
            if(consulClient.setKVValue(keyPath, "lock:" + LocalDateTime.now(), putParams).getValue()) {
                return true;
            } else if(block) {
                if(maxTimes != null && count >= maxTimes) {
                    return false;
                } else {
                    count ++;
                    if(timeInterval != null)
                        Thread.sleep(timeInterval);
                    continue;
                }
            } else {
                return false;
            }
        }
    }

    /**
     * 释放同步锁
     *
     * @return
     */
    public Boolean unlock() {
        if(checkTtl != null) {
            checkTtl.stop();
        }

        PutParams putParams = new PutParams();
        putParams.setReleaseSession(sessionId);
        boolean result = consulClient.setKVValue(keyPath, "unlock:" + LocalDateTime.now(), putParams).getValue();

        destroySession();
        return result;
    }


}
