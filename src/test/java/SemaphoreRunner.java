import com.ecwid.consul.v1.ConsulClient;
import com.didispace.lock.consul.CheckTtl;
import com.didispace.lock.consul.Semaphore;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Random;

@Slf4j
@AllArgsConstructor
public class SemaphoreRunner implements Runnable {

    private int flag;
    private CheckTtl checkTtl;

    @Override
    public void run() {
        Semaphore semaphore = new Semaphore(new ConsulClient(), 3, "mg-init", checkTtl);
        try {
            if (semaphore.acquired(true)) {
                // 获取到信号量，执行业务逻辑
                log.info("Thread {} start!", flag);
                Thread.sleep(new Random().nextInt(10000));
                log.info("Thread {} end!", flag);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // 信号量释放
                // Session锁释放
                // Session删除
                semaphore.release();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}