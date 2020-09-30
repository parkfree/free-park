package org.chenliang.freepark.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import org.chenliang.freepark.configuration.FreeParkConfig;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.rtmap.ParkDetail;
import org.chenliang.freepark.model.rtmap.Payment;
import org.chenliang.freepark.model.rtmap.PointDto;
import org.chenliang.freepark.model.rtmap.Status;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RtmapService {
  private final RestTemplate client;
  private final FreeParkConfig config;

  public RtmapService(RestTemplate client, FreeParkConfig config) {
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

  public void getPoint(Member member) {
    final PointDto param = PointDto.builder()
        .openid(member.getOpenId())
        .channelId(1001)
        .marketId("12964")
        .cardNo(member.getUserId())
        .mobile(member.getMobile())
        .build();
    final String jsonString = JSONObject.toJSONString(param);

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://appsmall.rtmap.com/sign/signRecord"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(jsonString))
        .build();
    try {
      HttpResponse<String> response = client.send(request,
          HttpResponse.BodyHandlers.ofString());
      log.info("Sign in response : {}", response.body());
    } catch (Exception e) {
      log.error("Sign in error", e);
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
