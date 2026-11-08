package com.sagademo.saga.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * The thread pool that runs sagas in the background.
 *
 * <p>Why background execution matters for this project: {@code POST /api/sagas/orders} answers
 * immediately with the {@code sagaId}, and the browser then polls
 * {@code GET /api/sagas/{sagaId}}. That is what lets the UI animate the timeline step by step
 * while the backend is really working - the animation follows real backend state, it does not
 * replace it.
 *
 * <p>The thread name prefix {@code saga-} also makes the console logs easy to read: every line
 * belonging to one distributed transaction comes from one {@code saga-N} thread.
 */
@Configuration
public class AsyncConfig {

    @Bean("sagaTaskExecutor")
    public TaskExecutor sagaTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("saga-");
        executor.initialize();
        return executor;
    }
}
