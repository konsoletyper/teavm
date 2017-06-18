/*
 *  Copyright 2015 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.classlib.impl.tz;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.classlib.impl.Base46;
import org.teavm.classlib.impl.CharFlow;
import org.teavm.jso.JSBody;
import org.teavm.platform.metadata.MetadataProvider;
import org.teavm.platform.metadata.ResourceMap;

public final class DateTimeZoneProvider {
    private static Map<String, DateTimeZone> cache = new HashMap<>();

    private DateTimeZoneProvider() {
    }

    public static DateTimeZone getTimeZone(String id) {
        if (!cache.containsKey(id)) {
            cache.put(id, createTimeZone(id));
        }
        return cache.get(id);
    }

    private static DateTimeZone createTimeZone(String id) {
        TimeZoneResource res = getTimeZoneResource(id);
        if (res == null) {
            return null;
        }
        String data = res.getData();
        CharFlow flow = new CharFlow(data.toCharArray());
        if (Base46.decodeUnsigned(flow) == StorableDateTimeZone.ALIAS) {
            String aliasId = data.substring(flow.pointer);
            return new AliasDateTimeZone(id, getTimeZone(aliasId));
        } else {
            return StorableDateTimeZone.read(id, data);
        }
    }

    public static String[] getIds() {
        List<String> ids = new ArrayList<>();
        for (String areaName : getResource().keys()) {
            ResourceMap<TimeZoneResource> area = getResource().get(areaName);
            for (String id : area.keys()) {
                if (!areaName.isEmpty()) {
                    id = areaName + "/" + id;
                }
                ids.add(id);
            }
        }
        return ids.toArray(new String[ids.size()]);
    }

    @GeneratedBy(DateTimeZoneProviderGenerator.class)
    private static native boolean timeZoneDetectionEnabled();

    public static DateTimeZone detectTimezone() {
        if (!timeZoneDetectionEnabled()) {
            return null;
        }

        List<Score> zones = new ArrayList<>();
        long time = System.currentTimeMillis();
        int offset = -getNativeOffset(System.currentTimeMillis());
        for (String id : getIds()) {
            DateTimeZone tz = getTimeZone(id);
            if (tz instanceof AliasDateTimeZone) {
                continue;
            }
            int tzOffset = tz.getOffset(time) / 60_000;
            if (Math.abs(tzOffset - offset) > 120 || tz.previousTransition(time) == time) {
                continue;
            }
            zones.add(new Score(tz));
        }

        List<Score> scoreTable = new ArrayList<>();
        scoreTable.addAll(zones);
        Map<Long, List<Score>> zoneMap = new HashMap<>();
        PriorityQueue<Long> queue = new PriorityQueue<>(zones.size(), new Comparator<Long>() {
            @Override public int compare(Long o1, Long o2) {
                return o2.compareTo(o1);
            }
        });
        Set<Long> timeInQueue = new HashSet<>();
        long last = time;
        queue.add(time);
        zoneMap.put(time, new ArrayList<>(zones));

        while (!queue.isEmpty() && scoreTable.size() > 1) {
            time = queue.remove();
            timeInQueue.remove(time);
            zones = zoneMap.remove(time);
            offset = -getNativeOffset(time);

            for (Score score : zones) {
                long prev = score.tz.previousTransition(time);
                if (prev == time) {
                    if (scoreTable.get(0) == score) {
                        return score.tz;
                    }
                    scoreTable.remove(score);
                } else {
                    int tzOffset = score.tz.getOffset(time) / 60_000;
                    if (Math.abs(tzOffset - offset) > 120) {
                        scoreTable.remove(score);
                        continue;
                    }
                    List<Score> prevZones = zoneMap.computeIfAbsent(prev, k -> new ArrayList<>());
                    prevZones.add(score);
                    if (timeInQueue.add(prev)) {
                        queue.add(prev);
                    }
                }
            }

            if (scoreTable.size() == 1 || scoreTable.get(0).tz.previousTransition(time) == time) {
                return scoreTable.get(0).tz;
            } else if (scoreTable.size() > 1 && scoreTable.get(0).value + 48 * 60 < scoreTable.get(1).value) {
                return scoreTable.get(0).tz;
            }

            for (int i = scoreTable.size() - 1; i >= 0; --i) {
                Score score = scoreTable.get(i);
                int tzOffset = score.tz.getOffset(time) / 60_000;
                if (tzOffset != offset) {
                    score.value += (int) ((last - time) / 60_000) * (Math.abs(tzOffset - offset)) / 30;
                }
                int j = i + 1;
                while (j < scoreTable.size() && score.value > scoreTable.get(j).value) {
                    scoreTable.set(j - 1, scoreTable.get(j));
                    ++j;
                }
                scoreTable.set(j - 1, score);
            }

            last = time;
        }

        return scoreTable.get(0).tz;
    }

    static class Score {
        DateTimeZone tz;
        int value;

        public Score(DateTimeZone tz) {
            this.tz = tz;
        }
    }

    private static TimeZoneResource getTimeZoneResource(String id) {
        String areaName;
        String locationName;
        int sepIndex = id.indexOf('/');
        if (sepIndex >= 0) {
            areaName = id.substring(0, sepIndex);
            locationName = id.substring(sepIndex + 1);
        } else {
            areaName = "";
            locationName = id;
        }
        if (!getResource().has(areaName)) {
            return null;
        }
        ResourceMap<TimeZoneResource> area = getResource().get(areaName);
        return area.has(locationName) ? area.get(locationName) : null;
    }

    @JSBody(params = "instant", script = "return new Date(instant).getTimezoneOffset();")
    private static native int getNativeOffset(double instant);

    @MetadataProvider(TimeZoneGenerator.class)
    private static native ResourceMap<ResourceMap<TimeZoneResource>> getResource();
}
