package org.chenliang.freepark.model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "tenants")
public class Tenant {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer id;
  private String carNumber;

  private String email;
  private String owner;
  @CreationTimestamp
  private LocalDateTime createdAt;
  @UpdateTimestamp
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "tenant", fetch = FetchType.EAGER)
  private List<Member> members;
}
