import com.ecwid.consul.v1.ConsulClient;
import com.didispace.lock.consul.CheckTtl;
import com.didispace.lock.consul.Lock;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;

@Slf4j
@AllArgsConstructor
public class LockRunner implements Runnable {


    private int flag;
    private CheckTtl checkTtl;

    @Override
    public void run() {
        Lock lock = new Lock(new ConsulClient(), "lock-session", "lock-key", checkTtl);
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