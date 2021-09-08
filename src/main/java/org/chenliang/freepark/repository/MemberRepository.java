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

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Integer> {
  List<Member> findByTenantId(Integer tenantId);

  List<Member> findByTenantIdAndEnablePayTrue(Integer tenantId);

  Member findFirstByEnablePayTrueAndTenant(Tenant tenant);

  Optional<Member> findFirstByIdAndTenantId(Integer id, Integer tenantId);

  boolean existsByIdAndTenantId(Integer id, Integer tenantId);

  @Query("SELECT m FROM Member m join fetch m.tenant t WHERE m.enablePoint = true AND (m.points < 2000 OR t.role = 'ROLE_ADMIN')")
  List<Member> findAllCheckInAllowedMembers();

  @EntityGraph(attributePaths = {"tenant"})
  Page<Member> findAll(Pageable pageable);

  List<Member> findByEnablePayTrue();

  @Modifying
  @Query("delete from Member m where m.tenant.id = ?1")
  void deleteInBulkByTenantId(Integer tenantId);

  Member findFirstByEnablePayTrue();

  Member findFirstByEnablePayTrueAndTenantOrderByCouponsDesc(Tenant tenant);

  long countByTenant(Tenant tenant);
}
