package org.hacker.freepark.controller.admin;

import org.hacker.freepark.exception.ResourceNotFoundException;
import org.hacker.freepark.model.CheckTask;
import org.hacker.freepark.model.entity.Tenant;
import org.hacker.freepark.repository.TenantRepository;
import org.hacker.freepark.service.CheckTaskManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admin")
public class AdminCheckTaskController {
  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private CheckTaskManager checkTaskManager;

  @PostMapping("/tenants/{id}/checktask")
  public CheckTask createCheckTask(@PathVariable Integer id) {
    return tenantRepository.findById(id).map(tenant -> {
      checkTaskManager.scheduleCheckTask(tenant, 0);
      return checkTaskManager.getTask(tenant);
    }).orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
  }

  @GetMapping("/tenants/{id}/checktask")
  public CheckTask getCheckTask(@PathVariable Integer id) {
    Tenant tenant = tenantRepository.findById(id)
                                    .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    return Optional.ofNullable(checkTaskManager.getTask(tenant))
        .orElseThrow(() -> new ResourceNotFoundException("Check task not found"));
  }

  @GetMapping("/checktasks")
  public List<CheckTask> getCheckTaskList() {
    return new ArrayList<>(checkTaskManager.getTasks());
  }

  @DeleteMapping("/tenants/{id}/checktask")
  public ResponseEntity<Void> cancelCheckTask(@PathVariable Integer id) {
    Tenant tenant = tenantRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    checkTaskManager.cancelCheckTask(tenant);
    return ResponseEntity.ok().build();
  }
}
