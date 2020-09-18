package org.chenliang.freepark.model.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "members")
public class Member {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  private String userId;
  private String openId;
  private String memType;
  private String mobile;
  private LocalDate lastPaidAt;
  @CreationTimestamp
  private LocalDateTime createdAt;
  @UpdateTimestamp
  private LocalDateTime updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  private Tenant tenant;

  @PrePersist
  void preInsert() {
    if (this.lastPaidAt == null) {
      this.lastPaidAt = LocalDate.of(2020, 1, 1);
    }
  }
}
