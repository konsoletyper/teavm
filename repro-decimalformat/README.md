# TeaVM DecimalFormat affix contract

This fixture covers the Unciv startup crash caused by `DecimalFormat` returning
null for omitted suffixes. It also checks explicit negative subpatterns,
`applyPattern` reuse, and the JDK behavior of explicitly null affix setters.

From the TeaVM checkout root, build the source classlib and style-check it:

```powershell
gradle :classlib:relocateJar :classlib:checkstyleMain
```

Then run the JDK reference and the generated-JavaScript contract:

```powershell
gradle -p repro-decimalformat clean jvmContract runJavaScriptContract
```

By default the fixture uses the full relocated source-built classlib JAR at
`classlib/build/libs/classlib-0.16.0-SNAPSHOT.jar`, matching Unciv's target
artifact. `-PclasslibJar=<path>` can select another full classlib JAR for a
comparison. The TeaVM task removes the published classlib entry and substitutes
this exact JAR, retaining normal dependency metadata. The out-of-process
compiler also receives the existing `teavm-platform` target entry required by
TeaVM's metadata generator.

The initial generated-JavaScript failure was reproduced with a copy of the
pre-fix source-built classlib artifact (SHA-256
`E860A693EEFEA8D3ADE239BC97BB69B720EEEDB2D72994C6CBAE6EB0156543C1`). That
artifact is kept locally under the ignored TeaVM-root `build/decimalformat-fixtures`
directory, so the fixture's `clean` task does not delete it. Rerun it with:

```powershell
gradle -p repro-decimalformat clean jvmContract runJavaScriptContract `
  -PclasslibJar=../build/decimalformat-fixtures/classlib-before-affix-fix.jar
```

After the fix, `:classlib:relocateJar` produced the selected relocated artifact
`classlib/build/libs/classlib-0.16.0-SNAPSHOT.jar` with SHA-256
`73014A4C8594E076812FDC5907293237C02CF35DA11C9A8243709CA87F1E4045`. The JDK
contract passed, the pre-fix generated-JavaScript contract failed on the
omitted suffix and null-setter checks, and the generated-JavaScript contract
passed with the relocated artifact.
