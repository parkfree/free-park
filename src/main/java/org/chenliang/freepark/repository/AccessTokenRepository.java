package org.chenliang.freepark.repository;

import org.chenliang.freepark.model.entity.AccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccessTokenRepository extends JpaRepository<AccessToken, Integer> {
  Optional<AccessToken> findByToken(String token);

  @Modifying
  @Query("delete from AccessToken a where a.tenant.id = ?1")
  void deleteInBulkByTenantId(Integer tenantId);
}
