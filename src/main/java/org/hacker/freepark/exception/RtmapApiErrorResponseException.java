package org.hacker.freepark.exception;

public class RtmapApiErrorResponseException extends RtmapApiException {
  private final int code;

  public RtmapApiErrorResponseException(int code, String message) {
    super(message);
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}
