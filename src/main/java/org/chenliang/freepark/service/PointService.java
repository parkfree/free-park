package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.exception.ResourceNotFoundException;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.rtmap.PointsResponse;
import org.chenliang.freepark.model.rtmap.Status;
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

    Status status;
    try {
      status = rtmapService.checkIn(member);
    } catch (Exception e) {
      log.error("Request Check in point API error for member {}", member.getMobile(), e);
      return;
    }

    if (status.getCode() != 200) {
      log.warn("Check in point failed for member {}, code: {}, message: {}", member.getMobile(),
          status.getCode(), status.getMsg());
      return;
    }

    log.info("Check in point success for member {}", member.getMobile());

    PointsResponse pointsResponse;
    try {
      pointsResponse = rtmapService.getAccountPoints(member);
    } catch (Exception e) {
      log.error("Request get account point API error for member {}", member.getMobile(), e);
      return;
    }

    if (pointsResponse.getStatus() != 200) {
      log.warn("Get account point for member {} failed, code: {}, message: {}", member.getMobile(),
          pointsResponse.getStatus(), pointsResponse.getMessage());
      return;
    }

    log.info("Get account point for member {} success, total points are: {}", member.getMobile(),
        pointsResponse.getTotal());

    member.setPoints(pointsResponse.getTotal());
    memberRepository.save(member);
  }
}
