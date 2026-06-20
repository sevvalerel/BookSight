package com.booksight.booksight.config;

import io.sentry.Sentry;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SentryConfig {

    @Value("${sentry.dsn:}")
    private String dsn;

    @Value("${sentry.send-default-pii:true}")
    private boolean sendDefaultPii;

    @PostConstruct
    public void init() {
        if (dsn != null && !dsn.isBlank()) {
            Sentry.init(options -> {
                options.setDsn(dsn);
                options.setSendDefaultPii(sendDefaultPii);
            });
        }
    }
}
