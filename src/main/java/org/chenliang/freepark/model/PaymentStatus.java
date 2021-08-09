package org.chenliang.freepark.model;

public enum PaymentStatus {
  SUCCESS,
  NO_AVAILABLE_MEMBER,
  CAR_NOT_FOUND,
  PARK_DETAIL_API_ERROR,
  PAY_API_ERROR,
  NO_NEED_TO_PAY,
  NEED_WECHAT_PAY,
  MEMBER_NO_DISCOUNT,
  RTMAP_API_ERROR,
}
