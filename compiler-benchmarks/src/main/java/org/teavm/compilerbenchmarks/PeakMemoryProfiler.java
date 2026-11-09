/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.compilerbenchmarks;

import com.sun.management.GarbageCollectionNotificationInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ScalarResult;

/**
 * Measures peak live heap size during an iteration. A background thread periodically forces full GC
 * with {@link System#gc()}, and the profiler records heap usage right after each of these collections,
 * when heap contains only reachable objects. Heap usage after regular collections is ignored, since it
 * includes garbage that was not collected yet.
 *
 * <p>Forced collections slow the benchmark down a lot, so don't compare time of runs with and without
 * this profiler. Short-lived spikes between two forced collections are not seen by the profiler.
 *
 * <p>Usage: {@code -prof org.teavm.compilerbenchmarks.PeakMemoryProfiler[:interval=<ms>]}, default
 * interval is 200 ms.
 */
public class PeakMemoryProfiler implements InternalProfiler {
    public static final String METRIC = "mem.peakLive";
    private static final String FORCED_GC_CAUSE = "System.gc()";

    private final long intervalMillis;
    private final Set<String> heapPools = new HashSet<>();
    private final List<NotificationEmitter> emitters = new ArrayList<>();
    private final AtomicLong peak = new AtomicLong(-1);
    private final AtomicInteger forcedCollections = new AtomicInteger();
    private final NotificationListener listener = this::handleNotification;
    private volatile boolean running;
    private Thread gcThread;

    public PeakMemoryProfiler() {
        this("");
    }

    public PeakMemoryProfiler(String initLine) {
        long interval = 200;
        for (var option : initLine.split("[;,]")) {
            option = option.trim();
            if (option.isEmpty()) {
                continue;
            }
            var eq = option.indexOf('=');
            var name = eq >= 0 ? option.substring(0, eq).trim() : option;
            var value = eq >= 0 ? option.substring(eq + 1).trim() : "";
            if (name.equals("interval")) {
                interval = Long.parseLong(value);
                if (interval <= 0) {
                    throw new IllegalArgumentException("Interval must be positive: " + value);
                }
            } else {
                throw new IllegalArgumentException("Unknown option of " + getClass().getSimpleName()
                        + ": " + name);
            }
        }
        intervalMillis = interval;
        for (var pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == MemoryType.HEAP) {
                heapPools.add(pool.getName());
            }
        }
    }

    @Override
    public String getDescription() {
        return "Peak live heap size, measured by periodically forced full GC";
    }

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        peak.set(-1);
        forcedCollections.set(0);
        for (var gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gc instanceof NotificationEmitter) {
                var emitter = (NotificationEmitter) gc;
                emitter.addNotificationListener(listener, null, null);
                emitters.add(emitter);
            }
        }
        running = true;
        gcThread = new Thread(this::forceCollections, "teavm-peak-memory-profiler");
        gcThread.setDaemon(true);
        gcThread.start();
    }

    @Override
    public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams,
            IterationParams iterationParams, IterationResult result) {
        running = false;
        gcThread.interrupt();
        try {
            gcThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        gcThread = null;
        for (var emitter : emitters) {
            try {
                emitter.removeNotificationListener(listener);
            } catch (ListenerNotFoundException e) {
                // ignore
            }
        }
        emitters.clear();
        if (forcedCollections.get() == 0) {
            // E.g. iteration is shorter than interval or explicit GC is disabled by -XX:+DisableExplicitGC
            return List.of();
        }
        return List.of(new ScalarResult(METRIC, peak.get(), "B", AggregationPolicy.AVG));
    }

    private void forceCollections() {
        while (running) {
            try {
                TimeUnit.MILLISECONDS.sleep(intervalMillis);
            } catch (InterruptedException e) {
                return;
            }
            if (!running) {
                return;
            }
            System.gc();
        }
    }

    private void handleNotification(Notification notification, Object handback) {
        if (!notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
            return;
        }
        var info = GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
        if (!info.getGcCause().equals(FORCED_GC_CAUSE)) {
            return;
        }
        long used = 0;
        for (var entry : info.getGcInfo().getMemoryUsageAfterGc().entrySet()) {
            if (heapPools.contains(entry.getKey())) {
                used += entry.getValue().getUsed();
            }
        }
        peak.accumulateAndGet(used, Math::max);
        forcedCollections.incrementAndGet();
    }
}
