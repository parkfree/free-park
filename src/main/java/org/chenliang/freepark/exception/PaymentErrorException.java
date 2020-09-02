package org.chenliang.freepark.exception;

import org.chenliang.freepark.model.PayStatus;

public class PaymentErrorException extends RuntimeException {
  private PayStatus payStatus;

  public PaymentErrorException(PayStatus payStatus) {
    this.payStatus = payStatus;
  }

  public PayStatus getPayStatus() {
    return payStatus;
  }
}
