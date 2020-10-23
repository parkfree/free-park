package org.chenliang.freepark.exception;

public class RtmapApiErrorResponseException extends RtmapApiException {
  private int code;

  public RtmapApiErrorResponseException(int code, String message) {
    super(message);
    this.code = code;
  }
}
