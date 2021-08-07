package org.chenliang.freepark.service;

import org.chenliang.freepark.configuration.FreeParkConfig;
import org.chenliang.freepark.exception.RtmapApiErrorResponseException;
import org.chenliang.freepark.exception.RtmapApiRequestErrorException;
import org.chenliang.freepark.model.entity.Member;
import org.chenliang.freepark.model.rtmap.ParkDetail;
import org.chenliang.freepark.testutil.DataUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class RtmapServiceTest {
  public static final String CAR_NUM = "京A66666";
  private RtmapService rtmapService;
  private RestTemplate restTemplate;

  @BeforeEach
  void setUp() {
    restTemplate = Mockito.mock(RestTemplate.class);
    FreeParkConfig config = new FreeParkConfig();
    config.setWxAppId("app-id");
    rtmapService = new RtmapService(restTemplate, config);
  }

  @Test
  void shouldGetParkDetail() {
    ParkDetail parkDetail = new ParkDetail();
    parkDetail.setCode(200);
    ResponseEntity<ParkDetail> responseEntity = new ResponseEntity<>(parkDetail, HttpStatus.OK);
    Member member = DataUtil.createMember("13688888888");
    Mockito.when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class),
                                       eq(ParkDetail.class), eq("app-id"), eq(member.getOpenId()),
                                       eq(CAR_NUM), eq(member.getUserId()), eq(member.getMemType())))
           .thenReturn(responseEntity);


    ParkDetail parkDetailResponse = rtmapService.getParkDetail(member, CAR_NUM);

    assertEquals(parkDetail, parkDetailResponse);
  }

  @Test
  void shouldThrowRtmapApiRequestErrorExceptionWhenGetParkThrowRestClientException() {
    Member member = DataUtil.createMember("13688888888");
    Mockito.when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class),
                                       eq(ParkDetail.class), eq("app-id"), eq(member.getOpenId()),
                                       eq(CAR_NUM), eq(member.getUserId()), eq(member.getMemType())))
           .thenThrow(new RestClientException("error"));

    assertThrows(RtmapApiRequestErrorException.class, () -> {
      rtmapService.getParkDetail(member, CAR_NUM);
    });
  }

  @Test
  void shouldThrowRtmapApiErrorResponseExceptionWhenGetParkThrowRestClientException() {
    ParkDetail parkDetail = new ParkDetail();
    parkDetail.setCode(400);
    parkDetail.setMsg("car not found");
    ResponseEntity<ParkDetail> responseEntity = new ResponseEntity<>(parkDetail, HttpStatus.OK);
    Member member = DataUtil.createMember("13688888888");
    Mockito.when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class),
                                       eq(ParkDetail.class), eq("app-id"), eq(member.getOpenId()),
                                       eq(CAR_NUM), eq(member.getUserId()), eq(member.getMemType())))
           .thenReturn(responseEntity);

    RtmapApiErrorResponseException exception = assertThrows(RtmapApiErrorResponseException.class, () -> {
      rtmapService.getParkDetail(member, CAR_NUM);
    });

    assertEquals(parkDetail.getCode(), exception.getCode());
    assertEquals(parkDetail.getMsg(), exception.getMessage());
  }
}