package org.chenliang.freepark.testutil;

import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.entity.Tenant;

public class DataUtil {
  public static Member createMember(String mobile) {
    Member member = new Member();
    member.setName("Lily");
    member.setTenant(createTenant("a@test.com", "京A66666", "ROLE_USER"));
    member.setMobile(mobile);
    member.setUserId("12bf4cccd4b94671935aa989a8bc59d3");
    member.setOpenId("bbw044abcdH70823CT32Lg2L2Dms");
    member.setPoints(0);
    member.setEnablePoint(true);
    return member;
  }

  public static Tenant createTenant(String email, String carNumber, String role) {
    Tenant tenant = new Tenant();
    tenant.setOwner("Gary");
    tenant.setEmail(email);
    tenant.setCarNumber(carNumber);
    tenant.setRole(role);
    tenant.setPassword("random");
    return tenant;
  }
}
