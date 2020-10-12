package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.configuration.FreeParkConfig;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.rtmap.ParkDetail;
import org.chenliang.freepark.model.rtmap.Payment;
import org.chenliang.freepark.model.rtmap.PointRequest;
import org.chenliang.freepark.model.rtmap.Status;
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
  private final RestTemplate client;
  private final FreeParkConfig config;

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

  @Retryable(value = RestClientException.class, maxAttempts = 3)
  public void getPoint(Member member) {
    final PointRequest pointRequest = PointRequest.builder()
        .openid(member.getOpenId())
        .channelId(1001)
        .marketId("12964")
        .cardNo(member.getUserId())
        .mobile(member.getMobile())
        .build();

    HttpEntity<PointRequest> request = new HttpEntity<>(pointRequest, createHeaders(member));

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
