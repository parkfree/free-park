package org.chenliang.freepark.model;

import lombok.Data;
import org.chenliang.freepark.model.entity.Payment;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static org.chenliang.freepark.repository.PaymentSpecifications.hasMemberId;
import static org.chenliang.freepark.repository.PaymentSpecifications.hasStatus;
import static org.chenliang.freepark.repository.PaymentSpecifications.hasTenantId;
import static org.chenliang.freepark.repository.PaymentSpecifications.paidAtFrom;
import static org.chenliang.freepark.repository.PaymentSpecifications.paidAtTo;

@Data
public class PaymentSearchQuery {
  private PaymentStatus status;
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime from;
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime to;
  private Integer tenantId;
  private Integer memberId;

  public Specification<Payment> toSpecification() {
    return Specification.where(hasStatus(status))
        .and(paidAtFrom(from))
        .and(paidAtTo(to))
        .and(hasTenantId(tenantId))
        .and(hasMemberId(memberId));
  }
}
