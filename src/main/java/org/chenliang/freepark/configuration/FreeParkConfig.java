package org.chenliang.freepark.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@ConfigurationProperties(prefix = "freepark")
@Configuration
@Data
public class FreeParkConfig {
  private String wxAppId;
  private String endpoint;
  private Map<String, String> headers;
  private Map<String, Integer> timeout;
}
