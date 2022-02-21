package org.hacker.freepark.repository;

import org.hacker.freepark.model.entity.Member;
import org.hacker.freepark.model.entity.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class MemberRepositoryTest {
  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  private MemberRepository memberRepository;

  @Test
  void shouldSelectCorrectMembersForCheckInTask() {
    Tenant normalUser = createTenant("user@example.com", "川A22222", "ROLE_USER");
    Tenant adminUser = createTenant("admin@example.com", "川A11111", "ROLE_ADMIN");
    createMember(normalUser, "1368888000", 1999, false);
    createMember(normalUser, "13688881111", 1999, true);
    createMember(normalUser, "13688882222", 2000, true);
    createMember(adminUser, "13688883333", 1999, true);
    createMember(adminUser, "13688884444", 2000, true);
    createMember(adminUser, "13688885555", 1999, false);
    List<Member> checkInAllowedMembers = memberRepository.findAllCheckInAllowedMembers();

    Set<String> resultSet = checkInAllowedMembers.stream().map(Member::getMobile).collect(Collectors.toSet());
    Set<String> expectedSet = Set.of("13688881111", "13688883333", "13688884444");

    assertEquals(expectedSet, resultSet);
  }

  @Test
  void shouldSelectTheLatestMemberThatEnablePay() {
    Tenant normalUser = createTenant("user@example.com", "川A22222", "ROLE_USER");
    createMember(normalUser, "1368888000", true);
    createMember(normalUser, "1368888001", true);
    createMember(normalUser, "1368888002", false);

    Member member = memberRepository.findFirstByEnablePayTrueOrderByIdDesc();

    assertEquals("1368888001", member.getMobile());
  }

  private Member createMember(Tenant tenant, String mobile, int points, boolean enablePoint) {
    Member member = new Member();
    member.setName("Lily");
    member.setTenant(tenant);
    member.setMobile(mobile);
    member.setUserId("12bf4cccd4b94671935aa989a8bc59d3");
    member.setOpenId("bbw044abcdH70823CT32Lg2L2Dms");
    member.setPoints(points);
    member.setEnablePoint(enablePoint);
    return memberRepository.save(member);
  }

  private Member createMember(Tenant tenant, String mobile,  boolean enablePay) {
    Member member = new Member();
    member.setName("Lily");
    member.setTenant(tenant);
    member.setMobile(mobile);
    member.setUserId("12bf4cccd4b94671935aa989a8bc59d3");
    member.setOpenId("bbw044abcdH70823CT32Lg2L2Dms");
    member.setPoints(0);
    member.setEnablePoint(true);
    member.setEnablePay(enablePay);
    return memberRepository.save(member);
  }

  private Tenant createTenant(String email, String carNumber, String role) {
    Tenant tenant = new Tenant();
    tenant.setOwner("Gary");
    tenant.setEmail(email);
    tenant.setCarNumber(carNumber);
    tenant.setRole(role);
    tenant.setPassword("random");
    return tenantRepository.save(tenant);
  }
}