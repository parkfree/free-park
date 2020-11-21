package org.chenliang.freepark.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Integer> {

  default Member getBestPayMember(Tenant tenant) {
    return getBestPayMember(tenant, 200);
  }

  default Member getBestPayMember(Tenant tenant, int point) {
    LocalDate today = LocalDate.now();
    Member member = findFirstByEnablePayIsTrueAndLastPaidAtBeforeAndTenantOrderByPointsDesc(today, tenant);
    if (member == null) {
      member = findFirstByEnablePayIsTrueAndPointsGreaterThanEqualAndTenantOrderByPointsDesc(point, tenant);
    }
    return member;
  }

  Member findFirstByEnablePayIsTrueAndPointsGreaterThanEqualAndTenantOrderByPointsDesc(int point, Tenant tenant);

  Member findFirstByEnablePayIsTrueAndLastPaidAtBeforeAndTenantOrderByPointsDesc(LocalDate date, Tenant tenant);

  List<Member> findByTenantId(Integer tenantId);

  Optional<Member> findFirstByIdAndTenantId(Integer id, Integer tenantId);

  boolean existsByIdAndTenantId(Integer id, Integer tenantId);

  @Query("SELECT m FROM Member m join fetch m.tenant t WHERE m.enablePoint = true AND (m.points < 2000 OR t.role = 'ROLE_ADMIN')")
  List<Member> findAllCheckInAllowedMembers();

  @EntityGraph(attributePaths = {"tenant"})
  Page<Member> findAll(Pageable pageable);

  @Modifying
  @Query("delete from Member m where m.tenant.id = ?1")
  void deleteInBulkByTenantId(Integer tenantId);
}
