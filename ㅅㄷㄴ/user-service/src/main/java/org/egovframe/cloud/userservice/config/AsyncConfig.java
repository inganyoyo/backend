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
     * 세션 처리용 비동기 Executor
     */
    @Bean("sessionAsyncExecutor")
    public Executor sessionAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);       // 기본 스레드 수
        executor.setMaxPoolSize(4);        // 최대 스레드 수
        executor.setQueueCapacity(100);    // 큐 용량
        executor.setThreadNamePrefix("SessionAsync-");
        executor.setKeepAliveSeconds(60);  // idle 스레드 유지 시간
        
        // 스레드 팩토리 설정 (로깅 용도)
        executor.setThreadFactory(r -> {
            Thread thread = new Thread(r);
            thread.setName("SessionAsync-" + thread.getId());
            log.debug("새 SessionAsync 스레드 생성: {}", thread.getName());
            return thread;
        });
        
        executor.initialize();
        log.info("SessionAsync ThreadPool 초기화 완료: core={}, max={}, queue={}", 
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
}
