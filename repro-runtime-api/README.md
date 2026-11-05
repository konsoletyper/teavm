# Thread and embedded resource API contract

The Java 21 reference and generated TeaVM 0.16 JavaScript checks cover thread lifecycle,
restart rejection, active enumeration, context-loader inheritance (including null), and
parent-first embedded resource lookup with duplicate classpath resources and independent
URL streams. They also check class lookup and escaped resource names.

Build the fork's `:classlib:jar :core:relocateJar`, then run from this directory:

```
gradle jvmContract checkstyleMain runJavaScriptContract -PclasslibJar=<absolute-fork-classlib-original.jar> -PcoreJar=<absolute-relocated-core.jar>
```

Use an immutable copied jar during parallel work. The JavaScript runner requires the success
marker, rejects errors, and has a watchdog. `RuntimeResources` explicitly retains the test
resources for TeaVM; the `duplicates` directory supplies a second classpath occurrence.

TeaVM uses closed-world class lookup, not dynamic bytecode loading. Resource URL additions
are JavaScript-only. Thread enumeration uses TeaVM's logical live-thread registry; this does
not add JVM thread groups or parallel native threads. No C/Wasm behavior is claimed by this
fixture.

Named class lookup follows TeaVM's reflection registration contract. The fixture explicitly
registers java.lang.String through ReflectionSupplier; unregistered dynamic names are not
promised loadable just because their bytecode was on the compile classpath.


Published core 0.16 fails registered dynamic class lookup in this fixture. The compiler fix
retains class-name metadata when registered classes are discovered (rather than only at
analyzer startup), propagates subsequently discovered lookup result types, and matches
the primitive boolean signature of Class.forName(String, boolean, ClassLoader). The patched
core plus classlib passes JVM, generated JavaScript, and Checkstyle checks.
