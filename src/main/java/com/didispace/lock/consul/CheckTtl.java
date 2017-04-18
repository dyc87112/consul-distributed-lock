package com.didispace.lock.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.OperationException;
import com.ecwid.consul.v1.agent.model.NewCheck;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author 翟永超
 * @create 2017/4/12.
 * @blog http://blog.didispace.com
 */
@Slf4j
public class CheckTtl {

    private ConsulClient consulClient;

    @Getter
    private String checkId;
    private NewCheck check;
    private Timer timer;

    private int ttlDelay = 5000;
    private int ttlPeriod = 10000;

    public CheckTtl(String checkId, ConsulClient consulClient) {
        this.checkId = checkId;
        this.consulClient = consulClient;
    }

    public void agentCheckRegister() {
        this.check = new NewCheck();
        check.setId(checkId);
        check.setName(checkId);
        check.setTtl("30s");
        check.setInterval("10s");
        check.setTimeout("10s");
        this.consulClient.agentCheckRegister(check);
    }

    public void agentCheckDegister() {
        if (this.checkId != null) {
            this.consulClient.agentCheckDeregister(checkId);
        }
    }


    public boolean isRunning() {
        if (this.timer == null) {
            return false;
        }
        return true;
    }

    public void start() {
        if (!isRunning()) {
            agentCheckRegister();
            consulClient.agentCheckPass(checkId);
            this.timer = new Timer();
            timer.scheduleAtFixedRate(new TtlTask(), ttlDelay, ttlPeriod);
        }
    }

    public void stop() {
        if (this.timer != null) {
            agentCheckDegister();
            timer.cancel();
        }
    }

    class TtlTask extends TimerTask {

        @Override
        public void run() {
            try {
                log.debug("{} run ttl...", checkId);
                consulClient.agentCheckPass(checkId);
            } catch (OperationException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
