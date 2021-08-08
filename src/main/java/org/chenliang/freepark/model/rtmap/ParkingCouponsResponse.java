package org.chenliang.freepark.model.rtmap;

import lombok.Data;

import java.util.List;

@Data
public class ParkingCouponsResponse {
  public static final int OK_CODE = 200;

  int code;
  String msg;
  InnerData data;

  @Data
  public static class InnerData {

    List<Coupon> couponList;
  }

  @Data
  public static class Coupon {

    String activityId;
    String activityName;
    String openId;
    String couponActivityId;
    int couponId;
    String qrCode;
    String getTime;
    int status;
    String statusDesc;
    int marketId;
    String mainInfo;
    int categoryId;
    String categoryDesc;
    String writeoffTime;
    int effectiveType;
    String effectiveStartTime;
    String effectiveEndTime;
    int activedLimitedStartDay;
    int activedLimitedDays;
    int facePrice;
    int unitPrice;
    int discount;
    int conditionType;
    int conditionPrice;
    String cost;
    String cid;
    String descClause;
    String issuerName;
    String imgLogoUrl;
    int parkingDeduction;
    int parkingDuration;
    boolean isUse;
    List<Image> couponImageList;
  }

  @Data
  public static class Image {

    int couponId;
    int imgType;
    String imgUrl;
  }
}
