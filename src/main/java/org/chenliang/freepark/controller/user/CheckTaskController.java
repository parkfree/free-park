package org.chenliang.freepark.controller.user;

import org.chenliang.freepark.exception.ResourceNotFoundException;
import org.chenliang.freepark.model.CheckTask;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.service.CheckTaskManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class CheckTaskController {
  @Autowired
  private CheckTaskManager checkTaskManager;

  @PostMapping("/checktask")
  public CheckTask createCheckTask(@AuthenticationPrincipal Tenant tenant) {
    checkTaskManager.scheduleCheckTask(tenant, 0);
    return checkTaskManager.getTask(tenant);
  }

  @GetMapping("/checktask")
  public CheckTask getCheckTask(@AuthenticationPrincipal Tenant tenant) {
    return Optional.ofNullable(checkTaskManager.getTask(tenant))
        .orElseThrow(() -> new ResourceNotFoundException("Check task not found"));
  }

  @DeleteMapping("/checktask")
  public ResponseEntity<Void> cancelCheckTask(@AuthenticationPrincipal Tenant tenant) {
    checkTaskManager.cancelCheckTask(tenant);
    return ResponseEntity.ok().build();
  }
}
