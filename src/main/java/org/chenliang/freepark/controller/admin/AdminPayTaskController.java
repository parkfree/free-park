package org.chenliang.freepark.controller.admin;

import org.chenliang.freepark.exception.ResourceNotFoundException;
import org.chenliang.freepark.model.PayTask;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.repository.TenantRepository;
import org.chenliang.freepark.service.PayTaskManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admin")
public class AdminPayTaskController {
  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private PayTaskManager payTaskManager;

  @GetMapping("/tenants/{id}/paytask")
  public PayTask getPayTask(@PathVariable Integer id) {
    Tenant tenant = tenantRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    return Optional.ofNullable(payTaskManager.getTask(tenant))
        .orElseThrow(() -> new ResourceNotFoundException("Pay task not found"));
  }

  @GetMapping("/paytasks")
  public List<PayTask> getPayTaskList() {
    return new ArrayList<>(payTaskManager.getTasks());
  }

  @DeleteMapping("/tenants/{id}/paytask")
  public ResponseEntity<Void> cancelPayTask(@PathVariable Integer id) {
    Tenant tenant = tenantRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    payTaskManager.cancelPayTask(tenant);
    return ResponseEntity.ok().build();
  }
}
