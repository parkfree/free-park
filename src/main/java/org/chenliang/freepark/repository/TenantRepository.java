package org.chenliang.freepark.repository;

import org.chenliang.freepark.model.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Integer> {
  Optional<Tenant> findByEmail(String email);
}
