package org.chenliang.freepark.exception;

public class RtmapApiException extends RuntimeException {
  private static final int UNEXPECTED = -1;
  private int code;

  public RtmapApiException(int code, String message) {
    super(message);
    this.code = code;
  }

  public RtmapApiException(Throwable cause) {
    super(cause);
    this.code = UNEXPECTED;
  }
}
