package com.fastcampus.ecommerce.config;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.io.IOException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailRetrierConfig {

  @Value("${email.retrier.max-attempts:3}")
  private Integer maxAttempts;

  @Value("${email.retrier.wait-duration:5s}")
  private Duration waitDuration;

  @Bean
  public Retry emailRetrier() {
    RetryConfig config = RetryConfig.custom()
        .maxAttempts(maxAttempts)
        .waitDuration(waitDuration)
        .retryExceptions(IOException.class)
        .build();
    return Retry.of("emailRetrier", config);
  }
}
