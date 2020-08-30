package org.chenliang.freepark.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "tenants")
public class Tenant {
  @Id
  @GeneratedValue(strategy= GenerationType.AUTO)
  private Integer id;
  private String carNumber;
  private String email;
  private String owner;

  @OneToMany(mappedBy = "tenant", fetch = FetchType.EAGER)
  private List<Member> members;

  @Temporal(TemporalType.TIMESTAMP)
  private Date createdAt;

  @Temporal(TemporalType.TIMESTAMP)
  private Date updatedAt;
}
