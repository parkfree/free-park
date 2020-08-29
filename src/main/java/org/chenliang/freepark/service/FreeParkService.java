package org.chenliang.freepark.service;

import org.chenliang.freepark.configuration.FreeParkConfig;
import org.chenliang.freepark.model.Member;
import org.chenliang.freepark.model.ParkDetail;
import org.chenliang.freepark.model.Payment;
import org.chenliang.freepark.model.Status;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
public class FreeParkService {
  private final RestTemplate client;
  private final FreeParkConfig config;

  public FreeParkService(RestTemplate client, FreeParkConfig config) {
    this.client = client;
    this.config = config;
  }

  public ParkDetail getParkDetail(Member member, String carNumber) {
    HttpHeaders httpHeaders = createHeaders(member);
    HttpEntity<Void> request = new HttpEntity<>(httpHeaders);

    ResponseEntity<ParkDetail> response = client.exchange(config.getUris().get("parkDetail"), HttpMethod.GET, request,
                                                          ParkDetail.class, config.getWxAppId(), member.getOpenId(),
                                                          carNumber, member.getUserId(), member.getMemType());
    return response.getBody();
  }

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
