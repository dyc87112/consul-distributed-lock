import com.didispace.lock.consul.CheckTtl;
import com.didispace.lock.consul.Lock;
import com.ecwid.consul.v1.ConsulClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Random;

/**
 * @author 翟永超
 * @create 2017/4/6.
 * @blog http://blog.didispace.com
 */

public class TestLock {

    @Test
    public void testLock() throws Exception  {
        ConsulClient consulClient = new ConsulClient();
        CheckTtl checkTtl = new CheckTtl("lock-1", consulClient);
        new Thread(new LockRunner(1, new CheckTtl("lock-1", consulClient))).start();
        new Thread(new LockRunner(2, new CheckTtl("lock-2", consulClient))).start();
        new Thread(new LockRunner(3, new CheckTtl("lock-3", consulClient))).start();
        new Thread(new LockRunner(4, new CheckTtl("lock-4", consulClient))).start();
        new Thread(new LockRunner(5, new CheckTtl("lock-5", consulClient))).start();
        Thread.sleep(30000L);
    }


}

@Slf4j
@AllArgsConstructor
class LockRunner implements Runnable {

    private int flag;
    private CheckTtl checkTtl;

    @Override
    public void run() {
        Lock lock = new Lock(new ConsulClient(), "lock-key", checkTtl);
        try {
            // 获取分布式互斥锁（参数含义：阻塞模式、每次尝试获取锁的间隔500ms、尝试n次）
            if (lock.lock(true, 500L, null)) {
                log.info("Thread {} start!", flag);
                // 处理业务逻辑
                Thread.sleep(new Random().nextInt(5000));
                log.info("Thread {} end!", flag);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

    }
}
