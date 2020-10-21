package org.chenliang.freepark.repository;

import org.chenliang.freepark.model.PaymentStatus;
import org.chenliang.freepark.model.entity.Payment;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class PaymentSpecifications {
  public static Specification<Payment> hasStatus(PaymentStatus status) {
    return status == null ? null : (root, query, builder) -> builder.equal(root.get("status"), status);
  }

  public static Specification<Payment> paidAtFrom(LocalDateTime from) {
    return from == null ? null : (root, query, builder) -> builder.greaterThan(root.get("paidAt"), from);
  }

  public static Specification<Payment> paidAtTo(LocalDateTime to) {
    return to == null ? null : (root, query, builder) -> builder.lessThan(root.get("paidAt"), to);
  }

  public static Specification<Payment> hasTenantId(Integer tenantId) {
    return tenantId == null ? null : (root, query, builder) -> builder.equal(root.join("tenant").get("id"), tenantId);
  }

  public static Specification<Payment> hasMemberId(Integer memberId) {
    return memberId == null ? null : (root, query, builder) -> builder.equal(root.join("member").get("id"), memberId);
  }
}
