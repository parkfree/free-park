package org.chenliang.freepark.controller;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.exception.PaymentErrorException;
import org.chenliang.freepark.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.persistence.EntityNotFoundException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ControllerAdvice
@Log4j2
public class ExceptionController {
  @ExceptionHandler(value = Exception.class)
  public ResponseEntity<ErrorResponse> internalErrorHandler(Exception e) {
    log.error("Unexpected exception", e);
    return response(INTERNAL_SERVER_ERROR, 999, e.getMessage());
  }

  @ExceptionHandler(value = PaymentErrorException.class)
  public ResponseEntity<ErrorResponse> paymentExceptionHandler(PaymentErrorException e) {
    return response(BAD_REQUEST, 100, e.getPayStatus().toString());
  }

  @ExceptionHandler(value = EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> notFoundExceptionHandler(PaymentErrorException e) {
    return response(NOT_FOUND, 101, e.getPayStatus().toString());
  }

  private ResponseEntity<ErrorResponse> response(HttpStatus status, Integer errorCode, String errorMessage) {
    ErrorResponse error = ErrorResponse.builder()
        .code(errorCode)
        .message(errorMessage)
        .build();
    return ResponseEntity.status(status).body(error);
  }
}
