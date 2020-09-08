package org.chenliang.freepark.controller;

import org.chenliang.freepark.exception.ResourceNotFoundException;
import org.chenliang.freepark.model.PayTask;
import org.chenliang.freepark.model.Tenant;
import org.chenliang.freepark.repository.TenantRepository;
import org.chenliang.freepark.service.PayTaskManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class PayTaskController {
  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private PayTaskManager payTaskManager;

  @GetMapping("/tenants/{id}/paytask")
  public PayTask getPayTask(@PathVariable Integer id) {
    Tenant tenant = tenantRepository.getOne(id);
    PayTask payTask = payTaskManager.getTask(tenant);
    if (payTask == null) {
      throw new ResourceNotFoundException("Pay task not found");
    }
    return payTask;
  }

  @GetMapping("/paytasks")
  public List<PayTask> getPayTaskList() {
    return new ArrayList<>(payTaskManager.getTasks());
  }

  @DeleteMapping("/tenants/{id}/paytask")
  public ResponseEntity<Void> cancelPayTask(@PathVariable Integer id) {
    Tenant tenant = tenantRepository.getOne(id);
    if (payTaskManager.getTask(tenant) != null) {
      payTaskManager.cancelPayTask(tenant);
    }
    return ResponseEntity.ok().build();
  }
}
