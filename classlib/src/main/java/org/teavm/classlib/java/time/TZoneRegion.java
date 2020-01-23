package org.teavm.classlib.java.time;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.time.zone.TZoneRules;
import org.teavm.classlib.java.util.TObjects;

final class TZoneRegion extends TZoneId implements TSerializable {

  private final String id;

  private final transient TZoneRules rules;

  static TZoneRegion ofId(String zoneId, boolean checkAvailable) {

    TObjects.requireNonNull(zoneId, "zoneId");
    checkName(zoneId);
    TZoneRules rules = null;
    return new TZoneRegion(zoneId, rules);
  }

  private static void checkName(String zoneId) {

    int n = zoneId.length();
    if (n < 2) {
      throw new TDateTimeException("Invalid ID for region-based ZoneId, invalid format: " + zoneId);
    }
    for (int i = 0; i < n; i++) {
      char c = zoneId.charAt(i);
      if (c >= 'a' && c <= 'z')
        continue;
      if (c >= 'A' && c <= 'Z')
        continue;
      if (c == '/' && i != 0)
        continue;
      if (c >= '0' && c <= '9' && i != 0)
        continue;
      if (c == '~' && i != 0)
        continue;
      if (c == '.' && i != 0)
        continue;
      if (c == '_' && i != 0)
        continue;
      if (c == '+' && i != 0)
        continue;
      if (c == '-' && i != 0)
        continue;
      throw new TDateTimeException("Invalid ID for region-based ZoneId, invalid format: " + zoneId);
    }
  }

  TZoneRegion(String id, TZoneRules rules) {

    this.id = id;
    this.rules = rules;
  }

  @Override
  public String getId() {

    return this.id;
  }

  @Override
  public TZoneRules getRules() {

    return this.rules;
  }

}
