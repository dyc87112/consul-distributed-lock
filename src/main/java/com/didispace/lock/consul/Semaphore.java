package com.didispace.lock.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.ecwid.consul.v1.kv.model.PutParams;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 基于Consul的信号量实现
 *
 * @author 翟永超
 * @create 2017/4/7.
 * @blog http://blog.didispace.com
 */
@Slf4j
public class Semaphore extends BaseLock {

    private static final String prefix = "semaphore/";  // 信号量参数前缀

    private int limit;
    private boolean acquired = false;


    /**
     * @param consulClient consul客户端实例
     * @param limit        信号量上限值
     * @param lockKey      信号量在consul中存储的参数路径
     * @param checkTtl      对锁Session的TTL
     */
    public Semaphore(ConsulClient consulClient, int limit, String lockKey, CheckTtl checkTtl) {
        super(consulClient, prefix + lockKey, checkTtl);
        this.limit = limit;
    }

    /**
     * acquired信号量
     *
     * @param block 是否阻塞。如果为true，那么一直尝试，直到获取到该资源为止。
     * @return
     * @throws IOException
     */
    public Boolean acquired(boolean block) throws IOException {
        if (acquired) {
            log.error("{} - Already acquired", sessionId);
            throw new RuntimeException(sessionId + " - Already acquired");
        }

        // destroy session and create new session
        destroySession();
        this.sessionId = createSession("semaphore" + this.keyPath);
        log.debug("Create session : {}", sessionId);

        // add contender entry
        String contenderKey = keyPath + "/" + sessionId;
        log.debug("contenderKey : {}", contenderKey);
        PutParams putParams = new PutParams();
        putParams.setAcquireSession(sessionId);
        Boolean b = consulClient.setKVValue(contenderKey, "", putParams).getValue();
        if (!b) {
            log.error("Failed to add contender entry : {}, {}", contenderKey, sessionId);
            throw new RuntimeException("Failed to add contender entry : {}, {}" + contenderKey + ", " + sessionId);
        }

        while (true) {
            // try to take the semaphore
            String lockKey = keyPath + "/.lock";
            GetValue lockKeyContent = consulClient.getKVValue(lockKey).getValue();
            if (lockKeyContent != null) {
                // get lock value
                ContenderValue contenderValue = ContenderValue.parse(lockKeyContent);

                // 当前信号量已满
                if (contenderValue.getLimit() == contenderValue.getHolders().size()) {

                    clearInvalidHolder(contenderValue);
                    if (block) {
                        // 如果是阻塞模式，再尝试
                        try {
                            Thread.sleep(500L);
                        } catch (InterruptedException e) {
                        }
                        continue;
                    }
                    // 非阻塞模式，直接返回没有获取到信号量
                    return false;
                }
                // 信号量增加
                contenderValue.getHolders().add(sessionId);
                putParams = new PutParams();
                putParams.setCas(lockKeyContent.getModifyIndex());
                boolean c = consulClient.setKVValue(lockKey, contenderValue.toString(), putParams).getValue();
                if (c) {
                    acquired = true;
                    return true;
                }
                continue;
            } else {
                // 当前信号量还没有，所以创建一个，并马上抢占一个资源
                ContenderValue contenderValue = new ContenderValue();
                contenderValue.setLimit(limit);
                contenderValue.getHolders().add(sessionId);

                putParams = new PutParams();
                putParams.setCas(0L);
                boolean c = consulClient.setKVValue(lockKey, contenderValue.toString(), putParams).getValue();
                if (c) {
                    acquired = true;
                    return true;
                }
                continue;
            }
        }
    }

    /**
     * 释放session、并从lock中移除当前的sessionId
     *
     * @throws IOException
     */
    public void release() throws IOException {
        if (this.acquired) {
            // remove session int /.lock's holders list
            while (true) {
                String contenderKey = keyPath + "/" + sessionId;
                String lockKey = keyPath + "/.lock";

                GetValue lockKeyContent = consulClient.getKVValue(lockKey).getValue();
                if (lockKeyContent != null) {
                    // lock值转换
                    ContenderValue contenderValue = ContenderValue.parse(lockKeyContent);
                    contenderValue.getHolders().remove(sessionId);
                    PutParams putParams = new PutParams();
                    putParams.setCas(lockKeyContent.getModifyIndex());
                    consulClient.deleteKVValue(contenderKey);
                    boolean c = consulClient.setKVValue(lockKey, contenderValue.toString(), putParams).getValue();
                    if (c) {
                        break;
                    }
                }
            }
        }
        // remove session key
        this.acquired = false;
        clearSession();
    }

    public void clearSession() {
        if (checkTtl != null) {
            checkTtl.stop();
        }

        destroySession();
    }


    public void clearInvalidHolder(ContenderValue contenderValue) throws IOException {
        log.debug("Semaphore limited {}, remove invalid session...", contenderValue.getLimit());

        // 获取/semaphore/<key>/下的所有竞争者session
        Map<String, String> aliveSessionMap = new HashMap<>();
        List<GetValue> sessionList = consulClient.getKVValues(keyPath).getValue();
        for (GetValue value : sessionList) {
            String session = value.getSession();
            if (session == null || value.getSession().isEmpty()) {
                continue;
            }
            aliveSessionMap.put(session, "");
        }

        String lockKey = keyPath + "/.lock";
        GetValue lockKeyContent = consulClient.getKVValue(lockKey).getValue();
        if (lockKeyContent != null) {
            // 清理holders中存储的不在semaphore/<key>/<session>中的session（说明该session已经被释放了）
            List<String> removeList = new LinkedList<>();
            for(int i = 0; i < contenderValue.getHolders().size(); i ++) {
                String holder = contenderValue.getHolders().get(i);
                if (!aliveSessionMap.containsKey(holder)) {
                    // 该session已经失效，需要从holder中剔除
                    removeList.add(holder);
                }
            }
            if (removeList.size() > 0) {
                contenderValue.getHolders().removeAll(removeList);
                // 清理失效的holder
                PutParams putParams = new PutParams();
                putParams.setCas(lockKeyContent.getModifyIndex());
                consulClient.setKVValue(lockKey, contenderValue.toString(), putParams).getValue();
            }
        }
    }

}
