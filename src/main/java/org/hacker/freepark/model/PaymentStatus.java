package org.hacker.freepark.model;

public enum PaymentStatus {
  SUCCESS,
  NO_AVAILABLE_MEMBER,
  CAR_NOT_FOUND,
  NO_NEED_TO_PAY,
  NEED_WECHAT_PAY,
  RTMAP_API_ERROR,
  @Deprecated
  MEMBER_NO_DISCOUNT,
  @Deprecated
  PARK_DETAIL_API_ERROR,
  @Deprecated
  PAY_API_ERROR,
}
