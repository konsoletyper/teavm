# TeaVM JavaScript throwable contract

This fixture checks Java `Throwable` stack behavior on the JDK and in generated JavaScript. The JVM run exercises the JDK implementation as a reference; the JS run uses the TeaVM fork's relocated core and classlib jars so it exercises the fork runtime rather than Maven Central's compiler/runtime artifacts.

From the TeaVM checkout, build the artifacts first:

```powershell
gradle :core:jar :core:relocateJar :classlib:jar :classlib:relocateJar :core:checkstyleMain :classlib:checkstyleMain
```

Copy the relocated outputs `core/build/libs/core-0.16.0-SNAPSHOT.jar` and `classlib/build/libs/classlib-0.16.0-SNAPSHOT.jar` into `repro-throwable/build/inputs/core.jar` and `classlib.jar`, or pass their paths as `-PcoreJar=... -PclasslibJar=...`. Do not use the stale `*-original.jar` core output for the JS run; `:core:relocateJar` produces the selected compiler artifact. Then run:

```powershell
gradle -p repro-throwable jvmContract checkstyleMain generateJavaScript runJavaScriptContract
```

The print-only entry point proves that `printStackTrace(PrintStream)` links stack-frame support even when the application never calls `getStackTrace()`:

```powershell
gradle -p repro-throwable -PteavmEntry="repro.PrintOnlyContract" checkstyleMain generateJavaScript runPrintOnlyContract
```

The full contract covers captured JS frames, V8 and Firefox-style line parsing, writable-stack behavior, array copying and null validation, and `fillInStackTrace`. Native frames use the `<JS>` class label and preserve only the engine's function, file, and line data; they do not claim Java source locations. If the host installs TeaVM's `$rt_decodeStack`, it remains the preferred decoder. The JVM run checks JDK reference semantics and does not validate TeaVM's classlib implementation.
