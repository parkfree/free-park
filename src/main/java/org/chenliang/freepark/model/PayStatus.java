package org.chenliang.freepark.model;

public enum PayStatus {
  SUCCESS,
  NO_AVAILABLE_MEMBER,
  CAR_NOT_FOUND,
  PARK_DETAIL_API_ERROR,
  ALREADY_PAID,
  PAY_API_ERROR,
  NEED_WECHAT_PAY
}
