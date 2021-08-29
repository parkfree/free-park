package org.chenliang.freepark.service;

import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.entity.Tenant;
import org.chenliang.freepark.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemberServiceTest {
  private MemberService memberService;
  private MemberRepository memberRepository;

  @BeforeEach
  void setUp() {
    memberRepository = Mockito.mock(MemberRepository.class);
    memberService = new MemberService(memberRepository);
  }

  @ParameterizedTest
  @MethodSource("provideMembers")
  void should_get_best_member(int receivableCent, List<Member> members) {
    Tenant tenant = createTenant();

    Mockito.when(memberRepository.findByTenantIdAndEnablePayTrue(1))
           .thenReturn(members);

    Member selectedMember = memberService.getBestMemberForPayment(tenant, receivableCent);
    assertEquals("good", selectedMember.getName());
  }

  private static Stream<Arguments> provideMembers() {
    return Stream.of(
        // one is not sufficient
        Arguments.of(1200, List.of(createMember(0, 2, "good"), createMember(0, 1, "bad"))),

        // -------- even receivable (4 hours) ------------
        // coupons: all are less than required
        Arguments.of(1200, List.of(createMember(400, 1, "good"), createMember(800, 0, "bad"))),
        Arguments.of(1200, List.of(createMember(800, 0, "bad"), createMember(400, 1, "good"))),

        // coupons: one less and one equal required
        Arguments.of(1200, List.of(createMember(0, 2, "good"), createMember(400, 1, "bad"))),
        Arguments.of(1200, List.of(createMember(400, 1, "bad"), createMember(0, 2, "good"))),

        // coupons: one less and one more
        Arguments.of(1200, List.of(createMember(0, 3, "good"), createMember(600, 1, "bad"))),
        Arguments.of(1200, List.of(createMember(600, 1, "bad"), createMember(0, 3, "good"))),

        // coupons: one more and one equal
        Arguments.of(1200, List.of(createMember(0, 2, "good"), createMember(0, 3, "bad"))),
        Arguments.of(1200, List.of(createMember(0, 3, "bad"), createMember(0, 2, "good"))),

        // coupons: all more than required
        Arguments.of(1200, List.of(createMember(0, 3, "good"), createMember(0, 4, "bad"))),
        Arguments.of(1200, List.of(createMember(0, 4, "bad"), createMember(0, 3, "good"))),

        // -------- odd receivable (5 hours) ------------
        // coupons: all are less than required
        Arguments.of(1500, List.of(createMember(600, 1, "good"), createMember(1000, 0, "bad"))),
        Arguments.of(1500, List.of(createMember(1000, 0, "bad"), createMember(600, 1, "good"))),

        // coupons: one less and one equal required
        Arguments.of(1500, List.of(createMember(800, 2, "good"), createMember(600, 1, "bad"))),
        Arguments.of(1500, List.of(createMember(600, 1, "bad"), createMember(800, 2, "good"))),

        // coupons: two equal
        Arguments.of(1500, List.of(createMember(800, 2, "good"), createMember(1000, 2, "bad"))),
        Arguments.of(1500, List.of(createMember(1000, 2, "bad"), createMember(800, 2, "good"))),

        // coupons: one less and one more
        Arguments.of(1500, List.of(createMember(0, 3, "good"), createMember(600, 1, "bad"))),
        Arguments.of(1500, List.of(createMember(600, 1, "bad"), createMember(0, 3, "good"))),

        // coupons: one more and one equal
        Arguments.of(1500, List.of(createMember(200, 2, "good"), createMember(0, 3, "bad"))),
        Arguments.of(1500, List.of(createMember(0, 3, "bad"), createMember(200, 2, "good"))),

        // coupons: all more than required
        Arguments.of(1500, List.of(createMember(0, 3, "good"), createMember(0, 4, "bad"))),
        Arguments.of(1500, List.of(createMember(0, 4, "bad"), createMember(0, 3, "good"))),

        // -------- odd receivable (1 hour) ------------
        Arguments.of(300, List.of(createMember(200, 1, "good"), createMember(400, 0, "bad"))),
        Arguments.of(300, List.of(createMember(400, 0, "bad"), createMember(200, 1, "good"))),

        Arguments.of(300, List.of(createMember(400, 0, "good"), createMember(0, 1, "bad"))),
        Arguments.of(300, List.of(createMember(0, 1, "bad"), createMember(400, 0, "good")))
    );
  }

  private static Member createMember(int points, int coupons, String name) {
    Member member = new Member();
    member.setPoints(points);
    member.setCoupons(coupons);
    member.setName(name);
    return member;
  }

  private Tenant createTenant() {
    Tenant tenant = new Tenant();
    tenant.setId(1);
    return tenant;
  }
}