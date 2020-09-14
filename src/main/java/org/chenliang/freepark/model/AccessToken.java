package org.chenliang.freepark.model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "access_tokens")
public class AccessToken {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  private Tenant tenant;
  private String token;
  private LocalDateTime expireAt;

  @CreationTimestamp
  private LocalDateTime createdAt;
}
