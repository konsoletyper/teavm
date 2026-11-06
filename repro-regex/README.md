# Regex replacement contract

This fixture covers the alternation-heavy regular expression and replacement used by Unciv's `KeyboardBinding.unCamelCase`, including unmatched capture groups and reuse of the same replacement across matches.

Build a paired core/classlib source artifact set, then copy the relocated jars into `build/inputs`:

```powershell
gradle :core:relocateJar :classlib:jar :classlib:relocateJar :classlib:checkstyleMain
Copy-Item classlib/build/libs/classlib-0.16.0-SNAPSHOT.jar repro-regex/build/inputs/classlib.jar
Copy-Item core/build/libs/core-0.16.0-SNAPSHOT.jar repro-regex/build/inputs/core.jar
```

Run the JDK and generated-JavaScript contracts:

```powershell
gradle -p repro-regex '-PclasslibJar=build/inputs/classlib.jar' '-PcoreJar=build/inputs/core.jar' jvmContract generateJavaScript runJavaScriptContract
```
