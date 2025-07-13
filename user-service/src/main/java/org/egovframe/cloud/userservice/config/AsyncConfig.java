package org.egovframe.cloud.userservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리를 위한 Thread Pool 설정
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * 세션 처리용 비동기 Executor (성능 최적화)
     */
    @Bean("sessionAsyncExecutor")
    public Executor sessionAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // CPU 코어 수에 기반한 동적 설정
        int coreCount = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(Math.max(4, coreCount));        // 최소 4개, CPU 코어 수만큼
        executor.setMaxPoolSize(Math.max(8, coreCount * 2));     // 최대 CPU*2
        executor.setQueueCapacity(500);                          // 100 → 500으로 증가
        executor.setThreadNamePrefix("SessionAsync-");
        executor.setKeepAliveSeconds(300);                       // 60초 → 300초로 증가
        
        // Rejection Policy 설정 (큐가 가득 찰 때 호출자 스레드에서 실행)
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        // Graceful Shutdown 설정
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        
        // 스레드 팩토리 설정 (로깅 용도)
        executor.setThreadFactory(r -> {
            Thread thread = new Thread(r);
            thread.setName("SessionAsync-" + thread.getId());

            return thread;
        });
        
        executor.initialize();
        log.info("SessionAsync ThreadPool 초기화 완료: core={}, max={}, queue={}, keepAlive={}s", 
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity(), 
                executor.getKeepAliveSeconds());
        
        return executor;
    }
}
