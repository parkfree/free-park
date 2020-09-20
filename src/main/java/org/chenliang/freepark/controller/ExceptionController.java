package org.chenliang.freepark.controller;

import lombok.extern.log4j.Log4j2;
import org.chenliang.freepark.exception.ErrorCodes;
import org.chenliang.freepark.exception.InvalidRequestException;
import org.chenliang.freepark.exception.PaymentErrorException;
import org.chenliang.freepark.exception.ResourceNotFoundException;
import org.chenliang.freepark.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.persistence.EntityNotFoundException;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.*;

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
    return response(BAD_REQUEST, ErrorCodes.PAYMENT_ERROR, e.getPaymentStatus().toString());
  }

  @ExceptionHandler(value = InvalidRequestException.class)
  public ResponseEntity<ErrorResponse> invalidRequestExceptionHandler(InvalidRequestException e) {
    return response(BAD_REQUEST, ErrorCodes.GENERIC_BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler(value = {EntityNotFoundException.class, ResourceNotFoundException.class})
  public ResponseEntity<ErrorResponse> notFoundExceptionHandler(Exception e) {
    return response(NOT_FOUND, ErrorCodes.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationExceptions(
      MethodArgumentNotValidException e) {
    Map<String, String> errors = new HashMap<>();
    e.getBindingResult().getAllErrors().forEach((error) -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });
    ResponseEntity<ErrorResponse> response = response(BAD_REQUEST, ErrorCodes.INVALID_PARAMETERS, "Invalid parameters");
    response.getBody().setDetails(errors);
    return response;
  }

  @ExceptionHandler(value = {AccessDeniedException.class})
  public ResponseEntity<ErrorResponse> accessDeniedExceptionHandler(Exception e) {
    return response(FORBIDDEN, ErrorCodes.FORBIDDEN, e.getMessage());
  }

  private ResponseEntity<ErrorResponse> response(HttpStatus status, Integer errorCode, String errorMessage) {
    ErrorResponse error = ErrorResponse.builder()
        .code(errorCode)
        .message(errorMessage)
        .build();
    return ResponseEntity.status(status).body(error);
  }
}
