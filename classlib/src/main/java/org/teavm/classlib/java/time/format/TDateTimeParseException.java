package org.teavm.classlib.java.time.format;

import org.teavm.classlib.java.time.TDateTimeException;

public class TDateTimeParseException extends TDateTimeException {

  private final String parsedString;

  private final int errorIndex;

  public TDateTimeParseException(String message, CharSequence parsedData, int errorIndex) {

    super(message);
    this.parsedString = parsedData.toString();
    this.errorIndex = errorIndex;
  }

  public TDateTimeParseException(String message, CharSequence parsedData, int errorIndex, Throwable cause) {

    super(message, cause);
    this.parsedString = parsedData.toString();
    this.errorIndex = errorIndex;
  }

  public String getParsedString() {

    return this.parsedString;
  }

  public int getErrorIndex() {

    return this.errorIndex;
  }

}
