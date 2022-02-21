package org.hacker.freepark.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import lombok.Data;
import org.hacker.freepark.service.UnitUtil;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
  private String name;
  private int points;
  private int coupons;
  private boolean enablePay;
  private boolean enablePoint;
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

  public int affordableParkingHour() {
    return UnitUtil.couponToHour(coupons) + UnitUtil.pointToHour(points);
  }
}
