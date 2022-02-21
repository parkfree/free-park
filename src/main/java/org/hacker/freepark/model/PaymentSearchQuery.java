package org.hacker.freepark.model;

import lombok.Data;
import org.hacker.freepark.model.entity.Payment;
import org.hacker.freepark.repository.PaymentSpecifications;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class PaymentSearchQuery {
  private PaymentStatus status;
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime from;
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime to;
  private Integer tenantId;
  private Integer memberId;
  private String carNumber;
  private String mobile;

  public Specification<Payment> toSpecification() {
    return Specification.where(PaymentSpecifications.hasStatus(status))
        .and(PaymentSpecifications.paidAtFrom(from))
        .and(PaymentSpecifications.paidAtTo(to))
        .and(PaymentSpecifications.hasTenantId(tenantId))
        .and(PaymentSpecifications.hasMemberId(memberId))
        .and(PaymentSpecifications.hasCarNumber(carNumber))
        .and(PaymentSpecifications.hasMobile(mobile));
  }
}
