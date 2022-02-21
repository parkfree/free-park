package org.hacker.freepark.configuration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {
  @Bean
  public RestTemplate freeParkClient(RestTemplateBuilder restTemplateBuilder, FreeParkConfig config) {
    return restTemplateBuilder.rootUri(config.getEndpoint())
        .defaultHeader("User-Agent", config.getHeaders().get("userAgent"))
        .defaultHeader("referer", config.getHeaders().get("referer"))
        .defaultHeader("Content-Type", "application/json")
        .setConnectTimeout(Duration.ofMillis(config.getTimeout().get("connect")))
        .setReadTimeout(Duration.ofMillis(config.getTimeout().get("read")))
        .build();
  }
}
