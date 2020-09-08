package org.chenliang.freepark.controller;

import org.chenliang.freepark.exception.ResourceNotFoundException;
import org.chenliang.freepark.model.CheckTask;
import org.chenliang.freepark.model.CheckTaskDto;
import org.chenliang.freepark.model.Tenant;
import org.chenliang.freepark.repository.TenantRepository;
import org.chenliang.freepark.service.CheckTaskManager;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class CheckTaskController {
  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private CheckTaskManager checkTaskManager;

  @Autowired
  private ModelMapper modelMapper;

  @PostMapping("/tenants/{id}/checktask")
  public CheckTaskDto createCheckTask(@PathVariable Integer id) {
    Tenant tenant = tenantRepository.getOne(id);
    checkTaskManager.scheduleCheckTask(tenant);

    CheckTask checkTask = checkTaskManager.getTask(tenant);
    return modelMapper.map(checkTask, CheckTaskDto.class);
  }

  @GetMapping("/tenants/{id}/checktask")
  public CheckTaskDto getCheckTask(@PathVariable Integer id) {
    Tenant tenant = tenantRepository.getOne(id);
    CheckTask checkTask = checkTaskManager.getTask(tenant);
    if (checkTask == null) {
      throw new ResourceNotFoundException("Check task not found");
    }
    return modelMapper.map(checkTask, CheckTaskDto.class);
  }

  @GetMapping("/checktasks")
  public List<CheckTaskDto> getCheckTaskList() {
    return checkTaskManager.getTasks().stream()
        .map(checkTask -> modelMapper.map(checkTask, CheckTaskDto.class))
        .collect(Collectors.toList());
  }

  @DeleteMapping("/tenants/{id}/checktask")
  public ResponseEntity<Void> cancelCheckTask(@PathVariable Integer id) {
    Tenant tenant = tenantRepository.getOne(id);
    if (checkTaskManager.getTask(tenant) != null) {
      checkTaskManager.cancelCheckTask(tenant);
    }
    return ResponseEntity.ok().build();
  }
}
