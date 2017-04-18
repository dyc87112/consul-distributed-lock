import com.ecwid.consul.v1.ConsulClient;
import com.didispace.lock.consul.CheckTtl;
import org.junit.Test;

/**
 * @author 翟永超
 * @create 2017/4/6.
 * @blog http://blog.didispace.com
 */

public class TestLock {

    @Test
    public void testLock() throws Exception  {
        ConsulClient consulClient = new ConsulClient();
        new Thread(new LockRunner(1, new CheckTtl("lock-1", consulClient))).start();
        new Thread(new LockRunner(2, new CheckTtl("lock-2", consulClient))).start();
        new Thread(new LockRunner(3, new CheckTtl("lock-3", consulClient))).start();
        new Thread(new LockRunner(4, new CheckTtl("lock-4", consulClient))).start();
        new Thread(new LockRunner(5, new CheckTtl("lock-5", consulClient))).start();
        Thread.sleep(20000L);
    }

    @Test
    public void testSemaphore() throws Exception {
        ConsulClient consulClient = new ConsulClient();
        new Thread(new SemaphoreRunner(1, new CheckTtl("semaphore-1", consulClient))).start();
        new Thread(new SemaphoreRunner(2, new CheckTtl("semaphore-2", consulClient))).start();
        new Thread(new SemaphoreRunner(3, new CheckTtl("semaphore-3", consulClient))).start();
        new Thread(new SemaphoreRunner(4, new CheckTtl("semaphore-4", consulClient))).start();
        new Thread(new SemaphoreRunner(5, new CheckTtl("semaphore-5", consulClient))).start();
        new Thread(new SemaphoreRunner(6, new CheckTtl("semaphore-6", consulClient))).start();
        new Thread(new SemaphoreRunner(7, new CheckTtl("semaphore-7", consulClient))).start();
        new Thread(new SemaphoreRunner(8, new CheckTtl("semaphore-8", consulClient))).start();
        new Thread(new SemaphoreRunner(9, new CheckTtl("semaphore-9", consulClient))).start();
        new Thread(new SemaphoreRunner(10, new CheckTtl("semaphore-10", consulClient))).start();
        Thread.sleep(50000L);
    }

}
