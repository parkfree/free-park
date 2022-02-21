package org.hacker.freepark.exception;

import org.hacker.freepark.model.PaymentStatus;

public class PaymentErrorException extends RuntimeException {
  private final PaymentStatus paymentStatus;

  public PaymentErrorException(PaymentStatus paymentStatus) {
    this.paymentStatus = paymentStatus;
  }

  public PaymentStatus getPaymentStatus() {
    return paymentStatus;
  }
}
