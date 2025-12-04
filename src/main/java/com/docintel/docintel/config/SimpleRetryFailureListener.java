package com.docintel.docintel.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SimpleRetryFailureListener implements RetryListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleRetryFailureListener.class);
    private final AtomicInteger retryCount = new AtomicInteger(0);

    @Override
    public <T, E extends Throwable> void onError(RetryContext context,
                                                 RetryCallback<T, E> callback, Throwable throwable) {
        int current = retryCount.incrementAndGet();
        switch (throwable) {
            case TransientAiException tae ->
                    LOGGER.warn("Transient AI error, retry #{}: {}", current, tae.getCause().getMessage());

            case ResourceAccessException rae ->
                    LOGGER.warn("Resource access issue, retry #{}: {}", current, rae.getCause().getMessage());

            default -> LOGGER.error("Will not retry: {}", throwable.getCause().getMessage());
        }
    }


}
