# Blocking queue and read/write lock contract

This fixture exercises `LinkedBlockingQueue` and `ReentrantReadWriteLock` with
the full TeaVM 0.16 classlib built from this checkout. The JVM run uses JDK
implementations as the reference; the generated JavaScript run uses TeaVM's
substitutions.

From the TeaVM checkout root, build the classlib first, then run:

```powershell
gradle :classlib:jar
gradle -p repro-concurrency clean jvmContract checkstyleMain runJavaScriptContract
```

The JS contract covers bounded and timed queue operations, producer/consumer
blocking, interruption and cancellation, read reentrancy, reader/writer
exclusion, fair waiter order, timed lock acquisition, and write-condition
signal/timeout/interruption behavior. This is targeted behavioral coverage,
not a claim that all `BlockingQueue`, `Lock`, or `Condition` API methods are
implemented.
