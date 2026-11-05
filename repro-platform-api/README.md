# File and path API contract

This isolated Java 21 reference and TeaVM 0.16 JavaScript fixture checks File.toPath caching,
relative/absolute normalization, Unicode paths, invalid NUL rejection, and DOS attribute
interface dispatch. It does not claim a DOS filesystem implementation.

Build the fork's `:classlib:jar`, then run from this directory:

```
gradle jvmContract checkstyleMain runJavaScriptContract -PclasslibJar=<absolute-fork-classlib-original.jar>
```

Use a copied jar while other agents edit the fork. The target dependency replaces published
teavm-classlib; a successful JVM run alone does not test the patch. The JavaScript runner
requires a success marker and fails on exceptions or timeout. Published 0.16 lacks File.toPath
and DosFileAttributes. The regression also exposed a pre-existing relative normalization
negative-index error in TDefaultPath, fixed alongside this API addition.
