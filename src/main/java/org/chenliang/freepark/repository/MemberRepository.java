package org.chenliang.freepark.repository;

import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<Member, Integer> {
  Member findFirstByLastPaidAtBeforeAndTenant(LocalDate date, Tenant tenant);
  List<Member> findByTenantId(Integer tenantId);
}
