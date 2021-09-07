package org.chenliang.freepark.service;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.configuration.FreeParkConfig;
import org.chenliang.freepark.exception.RtmapApiErrorResponseException;
import org.chenliang.freepark.exception.RtmapApiRequestErrorException;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.rtmap.BuyCouponResponse;
import org.chenliang.freepark.model.rtmap.BuyRequest;
import org.chenliang.freepark.model.rtmap.CheckInRequest;
import org.chenliang.freepark.model.rtmap.ParkDetail;
import org.chenliang.freepark.model.rtmap.ParkingCouponsResponse;
import org.chenliang.freepark.model.rtmap.ParkingCouponsResponse.Coupon;
import org.chenliang.freepark.model.rtmap.Payment;
import org.chenliang.freepark.model.rtmap.PointsResponse;
import org.chenliang.freepark.model.rtmap.ProductsResponse;
import org.chenliang.freepark.model.rtmap.RtmapMemberResponse;
import org.chenliang.freepark.model.rtmap.Status;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.chenliang.freepark.service.UnitUtil.pointToCent;

@Service
@Log4j2
public class RtmapService {

  private static final String MARKET_ID = "12964";
  private final RestTemplate client;
  private final FreeParkConfig config;

  public static final String PARK_DETAIL_URI = "/app-park/parkingFee/getDetailByCarNumber?wxAppId={wxAppId}" +
                                               "&openid={openid}&carNumber={carNumber}&userId={userId}" +
                                               "&memType={memType}";
  public static final String PAY_URI = "/app-park/payParkingFee";
  public static final String PRODUCT_LIST_URI = "/wxapp-integral/front/index/coupon/list?portalId={portalId}" +
                                                "&openId={openId}&industryId=&integralType=&collectionType=" +
                                                "&page={page}&pageSize=20&cid={cid}";
  public static final String BUY_COUPON_URI = "/wxapp-integral/front/index/buy";
  public static final String COUPON_LIST_URI = "/app-park/parkingFee/getParkingCouponCard?openid={openId}" +
                                               "&marketId={marketId}&cid={cid}&status=2";
  public static final String CHECK_IN_POINT_URI = "/sign/signRecord";
  public static final String GET_POINT_URI = "/wxapp-root/api/v1/score/account?tenantType=1&tenantId={tenantId}" +
                                             "&cid={cid}";
  public static final String GET_MEMBER_DETAIL_URI = "/wxapp-root/api/v1/customer/info?tenantType=1" +
                                                     "&tenantId={marketId}&searchType=1&searchText={mobile}";

  public RtmapService(RestTemplate client, FreeParkConfig config) {
    this.client = client;
    this.config = config;
  }

  public ParkDetail getParkDetail(Member member, String carNumber) {
    HttpHeaders httpHeaders = createHeaders(member);
    HttpEntity<Void> request = new HttpEntity<>(httpHeaders);

    ParkDetail parkDetail;
    try {
      parkDetail = client.exchange(PARK_DETAIL_URI, HttpMethod.GET, request, ParkDetail.class, config.getWxAppId(),
                                   member.getOpenId(), carNumber, member.getUserId(), member.getMemType())
                         .getBody();
    } catch (Exception e) {
      log.error("Call park detail API error for car {}", carNumber, e);
      throw new RtmapApiRequestErrorException(e);
    }

    if (!ParkDetail.OK_CODE.equals(parkDetail.getCode())) {
      log.warn("Call park detail API for car {} return error code: {}, message: {}",
               carNumber, parkDetail.getCode(), parkDetail.getMsg());
      throw new RtmapApiErrorResponseException(parkDetail.getCode(), parkDetail.getMsg());
    }

    return parkDetail;
  }

  public void payParkingFee(Member member, ParkDetail parkDetail, int points, List<Coupon> coupons) {
    String receiptVolume = coupons.stream().map(Coupon::getQrCode).collect(Collectors.joining(","));
    int receiptDeductible = coupons.stream().mapToInt(Coupon::getFacePrice).sum();
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
                             .scoreDeductible(pointToCent(points))
                             .scoreMinutes(0)
                             .receiptMinutes(0)
                             .memberDeductible(parkingFee.getMemberDeductible())
                             .memberMinutes(0)
                             .fullDeductible(0)
                             .fullMinutes(0)
                             .feeNumber(0)
                             .formId(randomHexHash())
                             .receiptVolume(receiptVolume)
                             .receiptDeductible(receiptDeductible)
                             .build();

    HttpEntity<Payment> request = new HttpEntity<>(payment, createHeaders(member));

    Status status;
    try {
      status = client.exchange(PAY_URI, HttpMethod.POST, request, Status.class).getBody();
    } catch (Exception e) {
      log.error("Call pay API error", e);
      throw new RtmapApiRequestErrorException(e);
    }

    if (status.getCode() != Status.PAY_OK_CODE) {
      log.warn("Call pay API return error code: {}, message: {}", status.getCode(), status.getMsg());
      throw new RtmapApiErrorResponseException(status.getCode(), status.getMsg());
    }
  }

  public void checkInPoint(Member member) {
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
      status = client.exchange(CHECK_IN_POINT_URI, HttpMethod.POST, request, Status.class)
                     .getBody();
    } catch (Exception e) {
      log.error("Call check in point API error for member {}", member.getMobile(), e);
      throw new RtmapApiRequestErrorException(e);
    }
    if (status.getCode() != Status.POINT_CHECKIN_OK_CODE && status.getCode() != Status.POINT_ALREADY_CHECKED_CODE) {
      log.warn("Call Check in point for member {} return error code: {}, message: {}",
               member.getMobile(), status.getCode(), status.getMsg());
      throw new RtmapApiErrorResponseException(status.getCode(), status.getMsg());
    }
  }

  public PointsResponse getAccountPoints(Member member) {
    HttpEntity<Void> headers = new HttpEntity<>(createHeaders(member));
    PointsResponse pointsResponse;
    try {
      pointsResponse = client.exchange(GET_POINT_URI, HttpMethod.GET, headers, PointsResponse.class,
                                       MARKET_ID, member.getUserId())
                             .getBody();
    } catch (Exception e) {
      log.error("Call get account point API error for member {}", member.getMobile(), e);
      throw new RtmapApiRequestErrorException(e);
    }

    if (pointsResponse.getStatus() != PointsResponse.OK_CODE) {
      log.warn("Call get account point API for member {} return error code: {}, message: {}",
               member.getMobile(), pointsResponse.getStatus(), pointsResponse.getMessage());
      throw new RtmapApiErrorResponseException(pointsResponse.getStatus(), pointsResponse.getMessage());
    }
    return pointsResponse;
  }

  public ProductsResponse getProducts(Member member, int page) {
    ProductsResponse response;
    try {
      HttpEntity<Void> request = new HttpEntity<>(createHeaders(member));
      response = client.exchange(PRODUCT_LIST_URI, HttpMethod.GET, request, ProductsResponse.class,
                                 MARKET_ID, member.getOpenId(), page, member.getUserId())
                       .getBody();
    } catch (Exception e) {
      log.error("Call product list API error with member {}", member.getMobile(), e);
      throw new RtmapApiRequestErrorException(e);
    }

    if (response.getStatus() != ProductsResponse.OK_CODE) {
      log.warn("Call product list API return error code: {}, message: {}", response.getStatus(),
               response.getMessage());
      throw new RtmapApiErrorResponseException(response.getStatus(), response.getMessage());
    }

    return response;
  }

  public void buyParkingCoupons(Member member, int productId, int number) {
    final BuyRequest buyRequest = BuyRequest.builder()
                                            .portalId(MARKET_ID)
                                            .openId(member.getOpenId())
                                            .appId(config.getWxAppId())
                                            .productId(productId)
                                            .cid(member.getUserId())
                                            .channelId(1035)
                                            .num(number)
                                            .build();

    BuyCouponResponse response;
    try {
      HttpEntity<BuyRequest> request = new HttpEntity<>(buyRequest, createHeaders(member));
      response = client.exchange(BUY_COUPON_URI, HttpMethod.POST, request, BuyCouponResponse.class).getBody();
    } catch (Exception e) {
      log.error("Call buy coupon API error for member {}", member.getMobile(), e);
      throw new RtmapApiRequestErrorException(e);
    }

    if (response.getStatus() != BuyCouponResponse.OK_CODE) {
      log.warn("Call buy coupon API for member {} return error code: {}, message: {}", member.getMobile(),
               response.getStatus(), response.getMessage());
      throw new RtmapApiErrorResponseException(response.getStatus(), response.getMessage());
    }
  }

  public ParkingCouponsResponse getAccountCoupons(Member member) {
    ParkingCouponsResponse response;
    try {
      HttpEntity<Void> headers = new HttpEntity<>(createHeaders(member));
      response = client.exchange(COUPON_LIST_URI, HttpMethod.GET, headers, ParkingCouponsResponse.class,
                                 member.getOpenId(), MARKET_ID, member.getUserId())
                       .getBody();
    } catch (Exception e) {
      log.error("Call get coupons API error for member {}", member.getMobile(), e);
      throw new RtmapApiRequestErrorException(e);
    }

    if (response.getCode() != ParkingCouponsResponse.OK_CODE) {
      log.warn("Call get coupons API for member {} return error code: {}, message: {}", member.getMobile(),
               response.getCode(), response.getMsg());
      throw new RtmapApiErrorResponseException(response.getCode(), response.getMsg());
    }

    return response;
  }

  public RtmapMemberResponse getMemberDetailByMobile(Member member, String mobile) {
    HttpEntity<Void> headers = new HttpEntity<>(createHeaders(member));
    RtmapMemberResponse response;
    try {
      response = client.exchange(GET_MEMBER_DETAIL_URI, HttpMethod.GET, headers, RtmapMemberResponse.class,
                                 MARKET_ID, mobile)
                       .getBody();
    } catch (Exception e) {
      log.error("Call get member detail API error for member {}", mobile, e);
      throw new RtmapApiRequestErrorException(e);
    }

    if (!RtmapMemberResponse.OK_CODE.equals(response.getStatus())) {
      log.warn("Call get member detail API for member {} return error code: {}, message: {}",
               mobile, response.getStatus(), response.getMessage());
      throw new RtmapApiErrorResponseException(response.getStatus(), response.getMessage());
    }
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
