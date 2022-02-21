package org.hacker.freepark;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FreeParkApplication {
  public static void main(String[] args) {
    SpringApplication.run(FreeParkApplication.class, args);
  }
}
