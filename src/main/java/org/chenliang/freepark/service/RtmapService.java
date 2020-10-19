package org.chenliang.freepark.service;

import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.configuration.FreeParkConfig;
import org.chenliang.freepark.model.MemberResponse;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.rtmap.ParkDetail;
import org.chenliang.freepark.model.rtmap.Payment;
import org.chenliang.freepark.model.rtmap.ScoreResponse;
import org.chenliang.freepark.model.rtmap.SignInRequest;
import org.chenliang.freepark.model.rtmap.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@Log4j2
public class RtmapService {

  private final RestTemplate client;
  private final FreeParkConfig config;

  @Autowired
  private MemberService memberService;

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
  public Status pay(Member member, ParkDetail parkDetail) {
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
      .memberDeductible(parkingFee.getMemberDeductible())
      .feeNumber(parkingFee.getFeeNumber())
      .formId(randomHexHash())
      .build();

    HttpEntity<Payment> request = new HttpEntity<>(payment, createHeaders(member));

    return client.exchange(config.getUris().get("pay"), HttpMethod.POST, request, Status.class).getBody();
  }

  @Retryable(value = RestClientException.class, maxAttempts = 2)
  public Status payWithScore(Member member, ParkDetail parkDetail) {
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
      .score(200)
      .scoreDeductible(300)
      .scoreMinutes(0)
      .receiptVolume("")
      .receiptDeductible(300)
      .receiptMinutes(0)
      .memberDeductible(parkingFee.getMemberDeductible())
      .memberMinutes(0)
      .fullDeductible(0)
      .fullMinutes(0)
      .feeNumber(parkingFee.getFeeNumber())
      .formId(randomHexHash())
      .build();

    HttpEntity<Payment> request = new HttpEntity<>(payment, createHeaders(member));

    return client.exchange(config.getUris().get("pay"), HttpMethod.POST, request, Status.class).getBody();
  }

  public void getPoint(Member member) {
    log.info("before sign in {} score is: {}", member.getMobile(), member.getScore());

    signIn(member);

    int newScore = getScore(member);

    updateScore(member, newScore);

    log.info("after sign in {} score is: {}", member.getMobile(), member.getScore());
  }

  @Retryable(value = RestClientException.class, maxAttempts = 3)
  public void signIn(Member member) {
    final SignInRequest signInRequest = SignInRequest.builder()
      .openid(member.getOpenId())
      .channelId(1001)
      .marketId("12964")
      .cardNo(member.getUserId())
      .mobile(member.getMobile())
      .build();

    try {
      HttpEntity<SignInRequest> request = new HttpEntity<>(signInRequest, createHeaders(member));
      Status status = client.exchange(config.getUris().get("checkInPoint"), HttpMethod.POST, request, Status.class).getBody();
      if (status != null) {
        if (status.getCode() == 200) {
          log.info("Check in point success for member {}", member.getMobile());
        } else {
          log.warn("Check in point failed for member {}, code: {}, message: {}", member.getMobile(),
            status.getCode(),
            status.getMsg());
        }
      } else {
        log.warn("Check in point failed for member {} with null status", member.getMobile());
      }
    } catch (Exception e) {
      log.error("Check in point request API error for member {}", member.getMobile(), e);
    }
  }

  @Retryable(value = RestClientException.class, maxAttempts = 3)
  public int getScore(Member member) {
    HttpEntity<Void> headers = new HttpEntity<>(createHeaders(member));
    try {
      ScoreResponse response = client.exchange(config.getUris().get("getScore"), HttpMethod.GET, headers, ScoreResponse.class,
        member.getTenant().getId(),
        member.getUserId()).getBody();
      if (response != null) {
        if (response.getStatus() == 200) {
          log.info("get score success for member {}", member.getMobile());
          return response.getTotal();
        } else {
          log.warn("update score failed for member {}, code: {}, message: {}",
            member.getMobile(),
            response.getStatus(),
            response.getMessage());
        }
      } else {
        log.warn("get score failed for member {} with null status", member.getMobile());
      }
    } catch (Exception ex) {
      log.error("get score request API error for member {}", member.getMobile(), ex);
    }
    return -1;
  }

  @Retryable(value = Exception.class, maxAttempts = 3)
  public void updateScore(Member member, int newScore) {
    if (newScore != -1 && member.getScore() != newScore) {
      MemberResponse memberResponse = memberService.updateScore(member, newScore);
      if (memberResponse != null) {
        log.info("update score successful for {}", member.getMobile());
      } else {
        log.info("update score failed for {}", member.getMobile());
      }
    } else {
      log.info("no need update score for {}", member.getMobile());
    }
  }

  private HttpHeaders createHeaders(Member member) {
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add("Token", creatToken(member.getUserId(), member.getOpenId()));
    return httpHeaders;
  }

  private String creatToken(String userId, String openId) {
    long timestamp = System.currentTimeMillis();

    return String.format("consumer=188880000002&timestamp=%d&nonce=%s&sign=%s&tenantId=12964&cid=%s&openId=%s&v=20200704",
      timestamp, randomHexHash(), randomHexHash(), userId, openId);
  }

  private String randomHexHash() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
