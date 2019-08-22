package com.photon.photonchain.network.excutor;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by yc on 2018/8/3.
 */
@Component
public class TotalTranExcutor extends ThreadPoolTaskExecutor {

    private int corePoolSize = 5;
    //线程池维护线程的最少数量

    private int maxPoolSize = 5;
    //线程池维护线程的最大数量

    private int queueCapacity = 5;
    //缓存队列

    private int keepAlive = 0;
    //允许的空闲时间


    public TotalTranExcutor() {
        super.setCorePoolSize(corePoolSize);
        super.setMaxPoolSize(maxPoolSize);
        super.setQueueCapacity(queueCapacity);
        super.setThreadNamePrefix("TotalTranExcutor-");
        // rejection-policy：当pool已经达到max size的时候，如何处理新任务
        // CALLER_RUNS：不在新线程中执行任务，而是由调用者所在的线程来执行
        super.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //对拒绝task的处理策略
        super.setKeepAliveSeconds(keepAlive);
        super.initialize();
    }



}
