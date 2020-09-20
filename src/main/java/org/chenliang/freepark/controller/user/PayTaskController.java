package org.chenliang.freepark.controller.user;

import org.chenliang.freepark.exception.ResourceNotFoundException;
import org.chenliang.freepark.model.PayTask;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.repository.TenantRepository;
import org.chenliang.freepark.service.PayTaskManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class PayTaskController {
  @Autowired
  private PayTaskManager payTaskManager;

  @GetMapping("/paytask")
  public PayTask getPayTask(@AuthenticationPrincipal Tenant tenant) {
    return Optional.ofNullable(payTaskManager.getTask(tenant))
        .orElseThrow(() -> new ResourceNotFoundException("Pay task not found"));
  }

  @DeleteMapping("/paytask")
  public ResponseEntity<Void> cancelPayTask(@AuthenticationPrincipal Tenant tenant) {
    payTaskManager.cancelPayTask(tenant);
    return ResponseEntity.ok().build();
  }
}
