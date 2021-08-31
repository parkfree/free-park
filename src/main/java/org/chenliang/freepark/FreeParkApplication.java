package org.chenliang.freepark;

import org.springdoc.core.SpringDocUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FreeParkApplication {
  public static void main(String[] args) {
//    SpringDocUtils.getConfig().replaceWithClass(org.springframework.data.domain.Pageable.class,
//                                                org.springdoc.core.converters.models.Pageable.class);

    SpringApplication.run(FreeParkApplication.class, args);
  }
}
