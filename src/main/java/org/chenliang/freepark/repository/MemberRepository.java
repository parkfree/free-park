package org.chenliang.freepark.repository;

import org.chenliang.freepark.model.Member;
import org.chenliang.freepark.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Integer> {
}
