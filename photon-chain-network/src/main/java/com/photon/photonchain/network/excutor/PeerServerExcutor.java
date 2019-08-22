package com.photon.photonchain.network.excutor;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Wu
 *
 * Created by SKINK on 2017/12/26.
 */
@Component
public class PeerServerExcutor extends ThreadPoolTaskExecutor {

  private int corePoolSize = 1;
  //线程池维护线程的最少数量

  private int maxPoolSize = 2;
  //线程池维护线程的最大数量

  private int queueCapacity = 1;
  //缓存队列

  private int keepAlive = 0;
  //允许的空闲时间

  public PeerServerExcutor() {
    super.setCorePoolSize(corePoolSize);
    super.setMaxPoolSize(maxPoolSize);
    super.setQueueCapacity(queueCapacity);
    super.setThreadNamePrefix("PeerServerExecutor-");
    // rejection-policy：当pool已经达到max size的时候，如何处理新任务
    // CALLER_RUNS：不在新线程中执行任务，而是由调用者所在的线程来执行
    super.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    //对拒绝task的处理策略
    super.setKeepAliveSeconds(keepAlive);
    super.initialize();
  }

}
