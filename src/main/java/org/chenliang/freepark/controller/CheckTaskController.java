package org.chenliang.freepark.controller;

import org.chenliang.freepark.exception.ResourceNotFoundException;
import org.chenliang.freepark.model.CheckTask;
import org.chenliang.freepark.model.CheckTaskDto;
import org.chenliang.freepark.model.Tenant;
import org.chenliang.freepark.repository.TenantRepository;
import org.chenliang.freepark.service.CheckTaskManager;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
