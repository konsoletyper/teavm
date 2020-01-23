package org.teavm.classlib.java.time.format;

public enum TTextStyle {

  FULL(2 /* Calendar.LONG_FORMAT */, 0), //
  FULL_STANDALONE(32770 /* Calendar.LONG_STANDALONE */, 0), //
  SHORT(1 /* Calendar.SHORT_FORMAT */, 1), //
  SHORT_STANDALONE(32769 /* Calendar.SHORT_STANDALONE */, 1), //
  NARROW(4 /* Calendar.NARROW_FORMAT */, 1), //
  NARROW_STANDALONE(32772 /* Calendar.NARROW_STANDALONE */, 1);

  private final int calendarStyle;

  private final int zoneNameStyleIndex;

  private TTextStyle(int calendarStyle, int zoneNameStyleIndex) {

    this.calendarStyle = calendarStyle;
    this.zoneNameStyleIndex = zoneNameStyleIndex;
  }

  public boolean isStandalone() {

    return (ordinal() & 1) == 1;
  }

  public TTextStyle asStandalone() {

    return TTextStyle.values()[ordinal() | 1];
  }

  public TTextStyle asNormal() {

    return TTextStyle.values()[ordinal() & ~1];
  }

  int toCalendarStyle() {

    return this.calendarStyle;
  }

  int zoneNameStyleIndex() {

    return this.zoneNameStyleIndex;
  }
}
