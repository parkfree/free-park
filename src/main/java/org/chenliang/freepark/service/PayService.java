package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.model.Member;
import org.chenliang.freepark.model.ParkDetail;
import org.chenliang.freepark.model.Status;
import org.chenliang.freepark.model.Tenant;
import org.chenliang.freepark.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@Log4j2
public class PayService {
  public static final Duration PAY_PERIOD = Duration.ofMinutes(60);
  private final Map<Integer, ScheduledFuture<?>> payTasks = new ConcurrentHashMap<>();

  @Autowired
  private MemberRepository memberRepository;

  @Autowired
  private RtmapService rtmapService;

  @Autowired
  private ThreadPoolTaskScheduler taskScheduler;

  public void schedulePayTask(Tenant tenant, Duration initialDelay) {
    ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(() -> {
      pay(tenant);
    }, Instant.now().plus(initialDelay), PAY_PERIOD);
    payTasks.put(tenant.getId(), future);
  }

  public void cancelPayTask(Tenant tenant) {
    boolean canceled = payTasks.get(tenant.getId()).cancel(false);
    payTasks.remove(tenant.getId());
    if (canceled) {
      log.info("Pay task for car {} is canceled", tenant.getCarNumber());
    } else {
      log.error("Cancel pay task for car {} failed", tenant.getCarNumber());
    }
  }

  public void pay(Tenant tenant) {
    LocalDate today = LocalDate.now();
    log.info("Start to pay car {}", tenant.getCarNumber());
    Member member = memberRepository.findFirstByLastPaidAtBeforeAndTenant(today, tenant);
    if (member == null) {
      log.warn("No available member for car: {}, cancel the pay schedule task.", tenant.getCarNumber());
      cancelPayTask(tenant);
      return;
    }

    ParkDetail parkDetail = getParkDetail(tenant, member);
    if (parkDetail == null) {
      return;
    }

    ParkDetail.ParkingFee parkingFee = parkDetail.getParkingFee();

    if (parkingFee.getReceivable() == 0) {
      log.info("Car {} is already paid", tenant.getCarNumber());
      return;
    }

    if (parkingFee.getMemberDeductible() == 0) {
      log.info("This member {} doesn't has discount for today. Update this member's last paid date, and try to pay with new member", member.getMobile());
      member.setLastPaidAt(today);
      memberRepository.save(member);
      pay(tenant);
      return;
    }

    if (parkingFee.getFeeNumber() != 0) {
      // TODO: notify owner
      log.info("Need manually pay {} CMB for car {}", (parkingFee.getFeeNumber() / 100), tenant.getCarNumber());
      return;
    }

    Status status;
    try {
      status = rtmapService.pay(member, parkDetail);
    } catch (Exception e) {
      log.error("Call pay API error", e);
      return;
    }

    if (status.getCode() == 401) {
      member.setLastPaidAt(today);
      memberRepository.save(member);
      log.info("Successfully paid car {} with member {}", tenant.getCarNumber(), member.getMobile());
    } else {
      log.warn("Pay car {} with member {} get error response: {}", tenant.getCarNumber(), member.getMobile(), status);
    }

    if (memberRepository.findFirstByLastPaidAtBeforeAndTenant(today, tenant) == null) {
      log.warn("No available member for car: {}, cancel the pay schedule task.", tenant.getCarNumber());
      cancelPayTask(tenant);
    }
  }

  private ParkDetail getParkDetail(Tenant tenant, Member member) {
    ParkDetail parkDetail;
    try {
      parkDetail = rtmapService.getParkDetail(member, tenant.getCarNumber());
    } catch (Exception e) {
      log.error("Call park detail API error", e);
      return null;
    }

    if (parkDetail.getCode() == 200) {
      return parkDetail;
    } else if (parkDetail.getCode() == 400) {
      log.info("The car is exit: {}", parkDetail.getMsg());
      cancelPayTask(tenant);
      return null;
    } else {
      log.warn("Call park detail API return unexpected error code: {}, message: {}", parkDetail.getCode(),
               parkDetail.getMsg());
      return null;
    }
  }
}
