package org.hacker.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.hacker.freepark.exception.ResourceNotFoundException;
import org.hacker.freepark.model.entity.Member;
import org.hacker.freepark.model.entity.Tenant;
import org.hacker.freepark.model.rtmap.PointsResponse;
import org.hacker.freepark.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
@Log4j2
public class PointService {

  public static final Supplier<ResourceNotFoundException> MEMBER_NOT_FOUND = () -> new ResourceNotFoundException("Member not found");
  @Autowired
  private MemberRepository memberRepository;

  @Autowired
  private RtmapService rtmapService;

  public void checkInPoint(int memberId) {
    Member member = memberRepository.findById(memberId)
                                    .orElseThrow(MEMBER_NOT_FOUND);
    checkInPoint(member);
  }

  public Member checkInPoint(Tenant tenant, Integer memberId) {
    Member member = memberRepository.findFirstByIdAndTenantId(memberId, tenant.getId())
                                    .orElseThrow(MEMBER_NOT_FOUND);
    checkInPoint(member);
    return memberRepository.findById(memberId)
                           .orElseThrow(MEMBER_NOT_FOUND);
  }

  public Member updatePoint(Tenant tenant, Integer memberId) {
    Member member = memberRepository.findFirstByIdAndTenantId(memberId, tenant.getId())
                                    .orElseThrow(MEMBER_NOT_FOUND);
    return updatePoint(member);
  }

  public Member updatePoint(Member member) {
    PointsResponse pointsResponse = rtmapService.getAccountPoints(member);
    if (member.getPoints() != pointsResponse.getTotal()) {
      log.info("Update member {} points from {} to {}",
               member.getMobile(), member.getPoints(), pointsResponse.getTotal());
      member.setPoints(pointsResponse.getTotal());
      return memberRepository.save(member);
    }
    return member;
  }

  private void checkInPoint(Member member) {
    try {
      rtmapService.checkInPoint(member);
    } finally {
      updatePoint(member);
    }
  }

}
