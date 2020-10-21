package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.configuration.FreeParkConfig;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.rtmap.CheckInRequest;
import org.chenliang.freepark.model.rtmap.ParkDetail;
import org.chenliang.freepark.model.rtmap.Payment;
import org.chenliang.freepark.model.rtmap.PointsResponse;
import org.chenliang.freepark.model.rtmap.Status;
import org.chenliang.freepark.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
@Log4j2
public class RtmapService {

  private static final String MARKET_ID = "12964";
  private final RestTemplate client;
  private final FreeParkConfig config;

  @Autowired
  private MemberRepository memberRepository;

  public RtmapService(RestTemplate client, FreeParkConfig config) {
    this.client = client;
    this.config = config;
  }

  @Retryable(value = RestClientException.class, maxAttempts = 2)
  public ParkDetail getParkDetail(Member member, String carNumber) {
    HttpHeaders httpHeaders = createHeaders(member);
    HttpEntity<Void> request = new HttpEntity<>(httpHeaders);

    ResponseEntity<ParkDetail> response = client.exchange(config.getUris().get("parkDetail"), HttpMethod.GET, request,
                                                          ParkDetail.class, config.getWxAppId(), member.getOpenId(),
                                                          carNumber, member.getUserId(), member.getMemType());
    return response.getBody();
  }

  @Retryable(value = RestClientException.class, maxAttempts = 2)
  public Status payWithPoints(Member member, ParkDetail parkDetail, int points) {
    ParkDetail.ParkingFee parkingFee = parkDetail.getParkingFee();
    Payment payment = Payment.builder()
        .wxAppId(config.getWxAppId())
        .openid(member.getOpenId())
        .carNumber(parkingFee.getCarNumber())
        .cardName(member.getMemType())
        .userId(member.getUserId())
        .marketOrderNumber(parkingFee.getMarketOrderNumber())
        .mobile(member.getMobile())
        .receivable(parkingFee.getReceivable())
        .score(points)
        .scoreDeductible(points / 200 * 300)
        .scoreMinutes(0)
        .receiptVolume("")
        .receiptDeductible(0)
        .receiptMinutes(0)
        .memberDeductible(parkingFee.getMemberDeductible())
        .memberMinutes(0)
        .fullDeductible(0)
        .fullMinutes(0)
        .feeNumber(0)
        .formId(randomHexHash())
        .build();

    HttpEntity<Payment> request = new HttpEntity<>(payment, createHeaders(member));

    return client.exchange(config.getUris().get("pay"), HttpMethod.POST, request, Status.class).getBody();
  }

  public void getPoint(Member member) {
    checkIn(member);
    int points = getLatestPoints(member);
    updatePoints(member, points);
    log.info("after sign in {} score is: {}", member.getMobile(), member.getPoints());
  }

  @Retryable(value = RestClientException.class, maxAttempts = 3)
  public void checkIn(Member member) {
    final CheckInRequest checkInRequest = CheckInRequest.builder()
        .openid(member.getOpenId())
        .channelId(1001)
        .marketId(MARKET_ID)
        .cardNo(member.getUserId())
        .mobile(member.getMobile())
        .build();

    HttpEntity<CheckInRequest> request = new HttpEntity<>(checkInRequest, createHeaders(member));
    try {
      Status status = client.exchange(config.getUris().get("checkInPoint"), HttpMethod.POST, request, Status.class).getBody();
      if (status.getCode() == 200) {
        log.info("Check in point success for member {}", member.getMobile());
      } else {
        log.warn("Check in point failed for member {}, code: {}, message: {}", member.getMobile(), status.getCode(), status.getMsg());
      }
    } catch (Exception e) {
      log.error("Check in point request API error for member {}", member.getMobile(), e);
    }
  }

  @Retryable(value = RestClientException.class, maxAttempts = 3)
  public int getLatestPoints(Member member) {
    HttpEntity<Void> headers = new HttpEntity<>(createHeaders(member));
    try {
      PointsResponse response = client.exchange(config.getUris().get("getPoints"), HttpMethod.GET, headers,
                                                PointsResponse.class, MARKET_ID, member.getUserId()).getBody();
      if (response.getStatus() == 200) {
        log.info("get score success for member {} total point is: {}", member.getMobile(), response.getTotal());
        return response.getTotal();
      } else {
        log.warn("Get account point failed for member {}, code: {}, message: {}", member.getMobile(), response.getStatus(), response.getMessage());
      }
    } catch (Exception e) {
      log.error("Get account point request API error for member {}", member.getMobile(), e);
    }
  }

  public void updatePoints(Member member, int points) {
    member.setPoints(points);
    memberRepository.save(member);
  }

  private HttpHeaders createHeaders(Member member) {
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add("Token", createToken(member.getUserId(), member.getOpenId()));
    return httpHeaders;
  }

  private String createToken(String userId, String openId) {
    long timestamp = System.currentTimeMillis();

    return String.format("consumer=188880000002&timestamp=%d&nonce=%s&sign=%s&tenantId=12964&cid=%s&openId=%s&v=20200704",
                         timestamp, randomHexHash(), randomHexHash(), userId, openId);
  }

  private String randomHexHash() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
