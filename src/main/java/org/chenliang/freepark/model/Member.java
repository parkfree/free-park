package org.chenliang.freepark.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "members")
public class Member {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer id;

  private String userId;
  private String openId;
  private String memType;
  private String mobile;
  private LocalDate lastPaidAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @ManyToOne
  private Tenant tenant;
}
