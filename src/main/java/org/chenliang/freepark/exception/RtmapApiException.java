package org.chenliang.freepark.exception;

public class RtmapApiException extends RuntimeException {
  public RtmapApiException(String message) {
    super(message);
  }

  public RtmapApiException(Throwable cause) {
    super(cause);
  }
}
