#基于Consul的分布式锁工具

在构建分布式系统的时候，我们经常需要控制对共享资源的访问。这个时候我们就涉及到分布式锁（也称为全局锁）的实现，本项目将基于Consul的KV存储来实现一套Java的分布式锁小工具，以帮助简化分布式环境下的同步操作。

## 基本原理

本工具实现的基本原理可以参见下面两篇博客：

- [基于Consul的分布式锁实现](http://blog.didispace.com/spring-cloud-consul-lock-and-semphore/)
- [基于Consul的分布式信号量实现](http://blog.didispace.com/spring-cloud-consul-lock-and-semphore-2/)

注意：这两篇博文仅描述了基本的实现流程，但未实现针对一些异常情况的锁和信号量清理，以解决出现死锁的情况，同时也为锁操作增加了一些其他情况的使用设置。

## 使用方法

目前该工具实现了两个小功能：分布式互斥锁和信号量。

### 分布式互斥锁

```java
ConsulClient consulClient = new ConsulClient("localhost", 8500);	// 创建与Consul的连接
CheckTtl checkTtl = new CheckTtl("checkId", consulClient); // session的健康检查，用来清理失效session占用的锁
Lock lock = new Lock(consulClient, "lockKey", checkTtl);
try {
	// 获取分布式互斥锁
  	// 参数含义：是否使用阻塞模式、每次尝试获取锁的间隔500ms、尝试n次
    if (lock.lock(true, 500L, null)) {     	
        // TODO 处理业务逻辑
    } 
catch (Exception e) {
    e.printStackTrace();
} finally {
    lock.unlock();
}
```

### 分布式信号量

```java
ConsulClient consulClient = new ConsulClient("localhost", 8500);	// 创建与Consul的连接
CheckTtl checkTtl = new CheckTtl("checkId", consulClient); // session的健康检查，用来清理失效session占用的锁
Semaphore semaphore = new Semaphore(consulClient, 3, "lockKey", checkTtl); // 3为信号量的值
try {
	if (semaphore.acquired(true)) {
    	// TODO 获取到信号量，执行业务逻辑
	}
} catch (Exception e) {
    e.printStackTrace();
} finally {
    try {
		// 信号量释放
		semaphore.release();
	} catch (IOException e) {
    	e.printStackTrace();
    }
}
```

### 持续优化ing

我的博客：http://blog.didispace.com

我的公众号：

![输入图片说明](https://git.oschina.net/uploads/images/2017/0418/232718_81992c15_437188.jpeg "在这里输入图片标题")

我的新书：

![输入图片说明](https://git.oschina.net/uploads/images/2017/0418/232734_5109a4d9_437188.png "在这里输入图片标题")