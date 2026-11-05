# Cooperative JavaScript executor and parking regression

Run `gradle checkstyleMain runJavaScriptRepro -PclasslibJar=<fork-original-jar>` after building
`:classlib:jar`. The fixture replaces the published class library with the complete fork,
rather than compiling selected source overlays that can hide replacements of existing classes.

This is a JavaScript scheduling regression. In particular, back-to-back unparks are expected
to coalesce while the cooperative event loop has not yielded. That timing assumption is not
a JVM concurrency contract, so this fixture is not advertised as a JVM reference test. The
separate scheduler, concurrency, runtime, and platform fixtures include JVM reference runs.

The runner fails for callback errors, missing success markers, or timeout. Dependent work
uses a constructed FutureTask rather than racing publication through an initially null array.
