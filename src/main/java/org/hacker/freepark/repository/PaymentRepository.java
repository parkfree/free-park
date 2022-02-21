package org.hacker.freepark.repository;

import org.hacker.freepark.model.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer>, JpaSpecificationExecutor<Payment> {
  @EntityGraph(attributePaths = {"member"})
  List<Payment> getByTenantIdAndPaidAtBetween(Integer tenantId, LocalDateTime begin, LocalDateTime end);

  @EntityGraph(attributePaths = {"member", "tenant"})
  Page<Payment> findAll(Pageable pageable);

  @EntityGraph(attributePaths = {"member", "tenant"})
  Page<Payment> findAll(Specification<Payment> specification, Pageable pageable);
}
