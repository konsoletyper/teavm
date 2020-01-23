package org.teavm.classlib.java.time;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.teavm.classlib.java.lang.TIllegalArgumentException;
import org.teavm.classlib.java.time.format.TDateTimeFormatterBuilder;
import org.teavm.classlib.java.time.format.TTextStyle;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQueries;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;
import org.teavm.classlib.java.time.temporal.TUnsupportedTemporalTypeException;
import org.teavm.classlib.java.time.zone.TZoneRules;
import org.teavm.classlib.java.util.TLocale;
import org.teavm.classlib.java.util.TMap;
import org.teavm.classlib.java.util.TObjects;

public abstract class TZoneId {

  private static Set<String> AVAILABLE_ZONE_IDS = Collections
      .unmodifiableSet(new HashSet<>(Arrays.asList("Z", "GMT", "GMT0", "UTC")));

  TZoneId() {

  }

  public abstract String getId();

  public String getDisplayName(TTextStyle style, TLocale locale) {

    return new TDateTimeFormatterBuilder().appendZoneText(style).toFormatter(locale).format(toTemporal());
  }

  private TTemporalAccessor toTemporal() {

    return new TTemporalAccessor() {
      @Override
      public boolean isSupported(TTemporalField field) {

        return false;
      }

      @Override
      public long getLong(TTemporalField field) {

        throw new TUnsupportedTemporalTypeException("Unsupported field: " + field);
      }

      @SuppressWarnings("unchecked")
      @Override
      public <R> R query(TTemporalQuery<R> query) {

        if (query == TTemporalQueries.zoneId()) {
          return (R) TZoneId.this;
        }
        return TTemporalAccessor.super.query(query);
      }
    };
  }

  public abstract TZoneRules getRules();

  public TZoneId normalized() {

    try {
      TZoneRules rules = getRules();
      if (rules.isFixedOffset()) {
        return rules.getOffset(TInstant.EPOCH);
      }
    } catch (RuntimeException ex) {
    }
    return this;
  }

  @Override
  public boolean equals(Object obj) {

    if (this == obj) {
      return true;
    }
    if (obj instanceof TZoneId) {
      TZoneId other = (TZoneId) obj;
      return getId().equals(other.getId());
    }
    return false;
  }

  @Override
  public int hashCode() {

    return getId().hashCode();
  }

  public static TZoneId systemDefault() {

    // return TTimeZone.getDefault().toZoneId();
    return TZoneOffset.UTC;
  }

  public static Set<String> getAvailableZoneIds() {

    return AVAILABLE_ZONE_IDS;
  }

  public static TZoneId of(String zoneId, TMap<String, String> aliasMap) {

    Objects.requireNonNull(zoneId, "zoneId");
    Objects.requireNonNull(aliasMap, "aliasMap");
    String id = Objects.requireNonNullElse(aliasMap.get(zoneId), zoneId);
    return of(id);
  }

  public static TZoneId of(String zoneId) {

    return of(zoneId, true);
  }

  public static TZoneId ofOffset(String prefix, TZoneOffset offset) {

    TObjects.requireNonNull(prefix, "prefix");
    TObjects.requireNonNull(offset, "offset");
    if (prefix.isEmpty()) {
      return offset;
    }

    if (!prefix.equals("GMT") && !prefix.equals("UTC") && !prefix.equals("UT")) {
      throw new TIllegalArgumentException("prefix should be GMT, UTC or UT, is: " + prefix);
    }

    if (offset.getTotalSeconds() != 0) {
      prefix = prefix.concat(offset.getId());
    }
    return new TZoneRegion(prefix, offset.getRules());
  }

  static TZoneId of(String zoneId, boolean checkAvailable) {

    TObjects.requireNonNull(zoneId, "zoneId");
    if (zoneId.length() <= 1 || zoneId.startsWith("+") || zoneId.startsWith("-")) {
      return TZoneOffset.of(zoneId);
    } else if (zoneId.startsWith("UTC") || zoneId.startsWith("GMT")) {
      return ofWithPrefix(zoneId, 3, checkAvailable);
    } else if (zoneId.startsWith("UT")) {
      return ofWithPrefix(zoneId, 2, checkAvailable);
    }
    return TZoneRegion.ofId(zoneId, checkAvailable);
  }

  private static TZoneId ofWithPrefix(String zoneId, int prefixLength, boolean checkAvailable) {

    String prefix = zoneId.substring(0, prefixLength);
    if (zoneId.length() == prefixLength) {
      return ofOffset(prefix, TZoneOffset.UTC);
    }
    if (zoneId.charAt(prefixLength) != '+' && zoneId.charAt(prefixLength) != '-') {
      return TZoneRegion.ofId(zoneId, checkAvailable);
    }
    try {
      TZoneOffset offset = TZoneOffset.of(zoneId.substring(prefixLength));
      if (offset == TZoneOffset.UTC) {
        return ofOffset(prefix, offset);
      }
      return ofOffset(prefix, offset);
    } catch (TDateTimeException ex) {
      throw new TDateTimeException("Invalid ID for offset-based ZoneId: " + zoneId, ex);
    }
  }

  public static TZoneId from(TTemporalAccessor temporal) {

    TZoneId obj = temporal.query(TTemporalQueries.zone());
    if (obj == null) {
      throw new TDateTimeException(
          "Unable to obtain ZoneId from TemporalAccessor: " + temporal + " of type " + temporal.getClass().getName());
    }
    return obj;
  }

}
