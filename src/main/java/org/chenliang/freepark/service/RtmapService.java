package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.configuration.FreeParkConfig;
import org.chenliang.freepark.exception.RtmapApiErrorResponseException;
import org.chenliang.freepark.exception.RtmapApiRequestErrorException;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.rtmap.BuyRequest;
import org.chenliang.freepark.model.rtmap.BuyResponse;
import org.chenliang.freepark.model.rtmap.CheckInRequest;
import org.chenliang.freepark.model.rtmap.CouponsResponse;
import org.chenliang.freepark.model.rtmap.CouponsResponse.Coupon;
import org.chenliang.freepark.model.rtmap.ParkDetail;
import org.chenliang.freepark.model.rtmap.Payment;
import org.chenliang.freepark.model.rtmap.PointsResponse;
import org.chenliang.freepark.model.rtmap.ProductsResponse;
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

import static org.chenliang.freepark.service.PaymentUtil.pointToCent;

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
  public Status payWithPoints(Member member, ParkDetail parkDetail, int points, Coupon coupon) {
    ParkDetail.ParkingFee parkingFee = parkDetail.getParkingFee();
    Payment.PaymentBuilder paymentBuilder = Payment.builder()
        .wxAppId(config.getWxAppId())
        .openid(member.getOpenId())
        .carNumber(parkingFee.getCarNumber())
        .cardName(member.getMemType())
        .userId(member.getUserId())
        .marketOrderNumber(parkingFee.getMarketOrderNumber())
        .mobile(member.getMobile())
        .receivable(parkingFee.getReceivable())
        .score(points)
        .scoreDeductible(pointToCent(points))
        .scoreMinutes(0)
        .receiptMinutes(0)
        .memberDeductible(parkingFee.getMemberDeductible())
        .memberMinutes(0)
        .fullDeductible(0)
        .fullMinutes(0)
        .feeNumber(0)
        .formId(randomHexHash());

    if (coupon != null) {
      paymentBuilder.receiptVolume(coupon.getQrCode())
          .receiptDeductible(coupon.getFacePrice());
    }

    Payment payment = paymentBuilder.build();

    HttpEntity<Payment> request = new HttpEntity<>(payment, createHeaders(member));

    return client.exchange(config.getUris().get("pay"), HttpMethod.POST, request, Status.class).getBody();
  }

  @Retryable(value = RtmapApiRequestErrorException.class, maxAttempts = 2)
  public void checkIn(Member member) {
    final CheckInRequest checkInRequest = CheckInRequest.builder()
        .openid(member.getOpenId())
        .channelId(1001)
        .marketId(MARKET_ID)
        .cardNo(member.getUserId())
        .mobile(member.getMobile())
        .build();

    HttpEntity<CheckInRequest> request = new HttpEntity<>(checkInRequest, createHeaders(member));

    Status status;
    try {
      status = client.exchange(config.getUris().get("checkInPoint"), HttpMethod.POST, request, Status.class).getBody();
    } catch (Exception e) {
      log.error("Request Check in point API error for member {}", member.getMobile(), e);
      throw new RtmapApiRequestErrorException(e);
    }
    // 200 is success, 400 is already check in
    if (status.getCode() != 200 && status.getCode() != 400) {
      log.warn("Check in point failed for member {}, code: {}, message: {}", member.getMobile(), status.getCode(),
               status.getMsg());
      throw new RtmapApiErrorResponseException(status.getCode(), status.getMsg());
    }
    log.info("Check in point success for member {}", member.getMobile());
  }

  @Retryable(value = RtmapApiRequestErrorException.class, maxAttempts = 2)
  public PointsResponse getAccountPoints(Member member) {
    HttpEntity<Void> headers = new HttpEntity<>(createHeaders(member));
    PointsResponse pointsResponse;
    try {
      pointsResponse = client.exchange(config.getUris().get("getPoints"), HttpMethod.GET, headers, PointsResponse.class,
                                       MARKET_ID, member.getUserId()).getBody();
    } catch (Exception e) {
      log.error("Request get account point API error for member {}", member.getMobile(), e);
      throw new RtmapApiRequestErrorException(e);
    }

    if (pointsResponse.getStatus() != 200) {
      log.warn("Get account point for member {} failed, code: {}, message: {}", member.getMobile(),
               pointsResponse.getStatus(), pointsResponse.getMessage());
      throw new RtmapApiErrorResponseException(pointsResponse.getStatus(), pointsResponse.getMessage());
    }
    log.info("Get account point for member {} success, total points are: {}", member.getMobile(), pointsResponse.getTotal());
    return pointsResponse;
  }

  public ProductsResponse getProducts(Member member, int page) {
    ProductsResponse response;
    try {
      HttpEntity<Void> headers = new HttpEntity<>(createHeaders(member));
      response = client.exchange(config.getUris().get("products"), HttpMethod.GET, headers, ProductsResponse.class, MARKET_ID,
                                 member.getOpenId(), page, member.getUserId()).getBody();
    } catch (Exception e) {
      log.error("An error occurred when calling the get products API for the member {}", member.getMobile(), e);
      throw new RtmapApiRequestErrorException(e);
    }

    if (response.getStatus() != 200) {
      log.warn("Failed to get products, code: {}, message: {}", response.getStatus(), response.getMessage());
      throw new RtmapApiErrorResponseException(response.getStatus(), response.getMessage());
    }

    log.info("Successfully get products page {} of {}", response.getData().getPage(), response.getData().getPages(), response);
    return response;
  }

  public void buy(Member member, int productId, int number) {
    final BuyRequest buyRequest = BuyRequest.builder()
        .portalId(MARKET_ID)
        .openId(member.getOpenId())
        .appId(config.getWxAppId())
        .productId(productId)
        .cid(member.getUserId())
        .channelId(1035)
        .num(number)
        .build();

    BuyResponse response;
    try {
      HttpEntity<BuyRequest> request = new HttpEntity<>(buyRequest, createHeaders(member));
      response = client.exchange(config.getUris().get("buy"), HttpMethod.POST, request, BuyResponse.class).getBody();
    } catch (Exception e) {
      log.error("An error occurred when calling the buy tickets API for the member {}", member.getMobile(), e);
      throw new RtmapApiRequestErrorException(e);
    }

    if (response.getStatus() != 200) {
      log.warn("Failed to buy tickets for the member {}, code: {}, message: {}", member.getMobile(), response.getStatus(),
               response.getMessage());
      throw new RtmapApiErrorResponseException(response.getStatus(), response.getMessage());
    }
    log.info("Successfully buy {} tickets for member {}", number, member.getMobile());
  }

  public CouponsResponse getCoupons(Member member) {
    CouponsResponse response;
    try {
      HttpEntity<Void> headers = new HttpEntity<>(createHeaders(member));
      response = client.exchange(config.getUris().get("coupons"), HttpMethod.GET, headers, CouponsResponse.class,
                                 member.getOpenId(), MARKET_ID, member.getUserId()).getBody();
    } catch (Exception e) {
      log.error("An error occurred when calling the get coupons API for the member {}", member.getMobile(), e);
      throw new RtmapApiRequestErrorException(e);
    }

    if (response.getCode() != 200) {
      log.warn("Failed to get coupons, code: {}, message: {}", response.getCode(), response.getMsg());
      throw new RtmapApiErrorResponseException(response.getCode(), response.getMsg());
    }

    log.info("Successfully get coupons {}", response);
    return response;
  }

  private HttpHeaders createHeaders(Member member) {
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add("Token", createToken(member.getUserId(), member.getOpenId()));
    return httpHeaders;
  }

  private String createToken(String userId, String openId) {
    long timestamp = System.currentTimeMillis();

    return String.format("consumer=188880000002&timestamp=%d&nonce=%s&sign=%s&tenantId=12964&cid=%s&openId=%s&v=20200708",
                         timestamp, randomHexHash(), randomHexHash(), userId, openId);
  }

  private String randomHexHash() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
