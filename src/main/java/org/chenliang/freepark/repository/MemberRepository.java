package org.chenliang.freepark.repository;

import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Integer> {
  default Member findFirstPayableMember(LocalDate date, Tenant tenant) {
    return findFirstByEnablePayIsTrueAndLastPaidAtBeforeAndTenantOrderByScoreDesc(date, tenant);
  }

  Member findFirstByEnablePayIsTrueAndLastPaidAtBeforeAndTenantOrderByScoreDesc(LocalDate date, Tenant tenant);

  List<Member> findByTenantId(Integer tenantId);

  Optional<Member> findFirstByIdAndTenantId(Integer id, Integer tenantId);

  boolean existsByIdAndTenantId(Integer id, Integer tenantId);

  List<Member> findByEnablePointIsTrue();

  @EntityGraph(attributePaths = {"tenant"})
  Page<Member> findAll(Pageable pageable);

  @Modifying
  @Query("delete from Member m where m.tenant.id = ?1")
  void deleteInBulkByTenantId(Integer tenantId);
}
