package org.teavm.classlib.java.time.temporal;

import static org.teavm.classlib.java.time.temporal.TChronoField.EPOCH_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.NANO_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.OFFSET_SECONDS;

import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalTime;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.chrono.TChronology;

public final class TTemporalQueries {
  private TTemporalQueries() {

  }

  public static TTemporalQuery<TZoneId> zoneId() {

    return TTemporalQueries.ZONE_ID;
  }

  public static TTemporalQuery<TChronology> chronology() {

    return TTemporalQueries.CHRONO;
  }

  public static TTemporalQuery<TTemporalUnit> precision() {

    return TTemporalQueries.PRECISION;
  }

  public static TTemporalQuery<TZoneId> zone() {

    return TTemporalQueries.ZONE;
  }

  public static TTemporalQuery<TZoneOffset> offset() {

    return TTemporalQueries.OFFSET;
  }

  public static TTemporalQuery<TLocalDate> localDate() {

    return TTemporalQueries.LOCAL_DATE;
  }

  public static TTemporalQuery<TLocalTime> localTime() {

    return TTemporalQueries.LOCAL_TIME;
  }

  static final TTemporalQuery<TZoneId> ZONE_ID = new TTemporalQuery<TZoneId>() {
    @Override
    public TZoneId queryFrom(TTemporalAccessor temporal) {

      return temporal.query(TTemporalQueries.ZONE_ID);
    }

    @Override
    public String toString() {

      return "ZoneId";
    }
  };

  static final TTemporalQuery<TChronology> CHRONO = new TTemporalQuery<TChronology>() {
    @Override
    public TChronology queryFrom(TTemporalAccessor temporal) {

      return temporal.query(TTemporalQueries.CHRONO);
    }

    @Override
    public String toString() {

      return "Chronology";
    }
  };

  static final TTemporalQuery<TTemporalUnit> PRECISION = new TTemporalQuery<TTemporalUnit>() {
    @Override
    public TTemporalUnit queryFrom(TTemporalAccessor temporal) {

      return temporal.query(TTemporalQueries.PRECISION);
    }

    @Override
    public String toString() {

      return "Precision";
    }
  };

  static final TTemporalQuery<TZoneOffset> OFFSET = new TTemporalQuery<TZoneOffset>() {
    @Override
    public TZoneOffset queryFrom(TTemporalAccessor temporal) {

      if (temporal.isSupported(OFFSET_SECONDS)) {
        return TZoneOffset.ofTotalSeconds(temporal.get(OFFSET_SECONDS));
      }
      return null;
    }

    @Override
    public String toString() {

      return "ZoneOffset";
    }
  };

  static final TTemporalQuery<TZoneId> ZONE = new TTemporalQuery<TZoneId>() {
    @Override
    public TZoneId queryFrom(TTemporalAccessor temporal) {

      TZoneId zone = temporal.query(ZONE_ID);
      return (zone != null ? zone : temporal.query(OFFSET));
    }

    @Override
    public String toString() {

      return "Zone";
    }
  };

  static final TTemporalQuery<TLocalDate> LOCAL_DATE = new TTemporalQuery<TLocalDate>() {
    @Override
    public TLocalDate queryFrom(TTemporalAccessor temporal) {

      if (temporal.isSupported(EPOCH_DAY)) {
        return TLocalDate.ofEpochDay(temporal.getLong(EPOCH_DAY));
      }
      return null;
    }

    @Override
    public String toString() {

      return "LocalDate";
    }
  };

  static final TTemporalQuery<TLocalTime> LOCAL_TIME = new TTemporalQuery<TLocalTime>() {
    @Override
    public TLocalTime queryFrom(TTemporalAccessor temporal) {

      if (temporal.isSupported(NANO_OF_DAY)) {
        return TLocalTime.ofNanoOfDay(temporal.getLong(NANO_OF_DAY));
      }
      return null;
    }

    @Override
    public String toString() {

      return "LocalTime";
    }
  };

}
