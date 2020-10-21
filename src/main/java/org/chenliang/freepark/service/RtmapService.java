package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.configuration.FreeParkConfig;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.rtmap.*;
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

  @Retryable(value = RestClientException.class, maxAttempts = 3)
  public Status checkIn(Member member) {
    final CheckInRequest checkInRequest = CheckInRequest.builder()
        .openid(member.getOpenId())
        .channelId(1001)
        .marketId(MARKET_ID)
        .cardNo(member.getUserId())
        .mobile(member.getMobile())
        .build();

    HttpEntity<CheckInRequest> request = new HttpEntity<>(checkInRequest, createHeaders(member));
    return client.exchange(config.getUris().get("checkInPoint"), HttpMethod.POST, request, Status.class).getBody();
  }

  @Retryable(value = RestClientException.class, maxAttempts = 3)
  public PointsResponse getAccountPoints(Member member) {
    HttpEntity<Void> headers = new HttpEntity<>(createHeaders(member));
    return client.exchange(config.getUris().get("getPoints"), HttpMethod.GET, headers, PointsResponse.class, MARKET_ID,
        member.getUserId()).getBody();
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
