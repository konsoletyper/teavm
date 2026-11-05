# TeaVM classlib replacement contract

This fixture checks the saturated `TTimeUnit` implementation and the JDK timed
`Future.get` behavior using the full classlib built from this checkout. It does
not compile a same-named source overlay into the application classpath.

From the TeaVM checkout root, build the source classlib and run its style check:

```powershell
gradle :classlib:jar :classlib:checkstyleMain
```

The output is `classlib/build/libs/classlib-0.16.0-SNAPSHOT-original.jar`.
Then generate and run the JavaScript contract with TeaVM 0.16.0 and Node:

```powershell
gradle -p repro-timeunit clean jvmContract runJavaScriptContract
```

The fixture replaces only the resolved `org.teavm:teavm-classlib:0.16.0` JAR
in the target classpath. It retains the normal TeaVM dependency graph. For the
out-of-process compiler, it also adds `teavm-platform` from the target
classpath to the daemon classpath so metadata generator classes are available;
the daemon classpath itself does not contain the classlib artifact.

The JVM contract checks the `TTimeUnit` class from the built JAR and JDK
`TimeUnit` behavior. The generated JavaScript contract reaches the same
`TTimeUnit` class through TeaVM's `java.util.concurrent.TimeUnit` substitution
and checks completed and incomplete futures with negative timeouts.
