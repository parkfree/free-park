package org.chenliang.freepark.service;

import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.exception.ResourceNotFoundException;
import org.chenliang.freepark.exception.RtmapApiRequestErrorException;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.rtmap.PointsResponse;
import org.chenliang.freepark.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class PointService {

  @Autowired
  private MemberRepository memberRepository;

  @Autowired
  private RtmapService rtmapService;

  public void getPoint(int memberId) {
    Member member = memberRepository.findById(memberId)
        .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

    rtmapService.checkIn(member);
    PointsResponse pointsResponse = rtmapService.getAccountPoints(member);
    int oldPoints = member.getPoints();
    if (oldPoints != pointsResponse.getTotal()) {
      member.setPoints(pointsResponse.getTotal());
      memberRepository.save(member);
      log.info("Update member {} points from {} to {}", member.getMobile(), oldPoints, pointsResponse.getTotal());
    }
  }

  public void refreshPoint(int tenantId) {
    final List<Member> members = memberRepository.findByTenantId(tenantId);
    members.forEach(member -> {
      try {
        PointsResponse pointsResponse = rtmapService.getAccountPoints(member);
        int oldPoints = member.getPoints();
        if (oldPoints != pointsResponse.getTotal()) {
          member.setPoints(pointsResponse.getTotal());
          memberRepository.save(member);
          log.info("Refresh member {} points from {} to {}", member.getMobile(), oldPoints, pointsResponse.getTotal());
        }
      } catch (RtmapApiRequestErrorException ex) {
        log.error("refresh member {} points failed, cause by {}", member.getMobile(), ex.getMessage(), ex);
      }
    });
  }
}
