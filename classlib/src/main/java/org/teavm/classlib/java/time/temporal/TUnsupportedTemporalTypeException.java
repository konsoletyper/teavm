package org.teavm.classlib.java.time.temporal;

import org.teavm.classlib.java.lang.TThrowable;
import org.teavm.classlib.java.time.TDateTimeException;

public class TUnsupportedTemporalTypeException extends TDateTimeException {

  public TUnsupportedTemporalTypeException(String message) {

    super(message);
  }

  public TUnsupportedTemporalTypeException(String message, TThrowable cause) {

    super(message, cause);
  }

}
