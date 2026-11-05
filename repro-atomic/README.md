# AtomicReferenceArray compatibility contract

This isolated test build compiles `TAtomicReferenceArray` from the sibling
classlib source tree against published TeaVM 0.15.0. It avoids provisioning
the JDK 25 required to configure the full TeaVM test suite. No local Maven
artifact or prebuilt compatibility JAR is used.

With JDK 21 and Gradle 9.4.1, from this directory:

```sh
gradle jvmContract generateJavaScript checkstyleMain --no-daemon
node -e "require('./build/generated/teavm/js/teavm-atomic-reference-array-repro.js').main([], error => { if (error) { console.error(error); process.exitCode = 1; } })"
```

Both JVM reference execution and generated JavaScript must print
`AtomicReferenceArray contract passed`. The contract covers constructor
copying, identity comparison, null values, update return values, memory-access
API variants, bounds/null errors, and two concurrent increment loops with
suspension inside the update callback. The JavaScript run validates the TeaVM
substitution, while the JVM run checks the same expectations against the JDK.

The implementation composes TeaVM's existing atomic-reference primitive per
array element. JavaScript cooperative-thread behavior is tested; Wasm and C
backends and hardware-thread memory ordering have not been validated here.
The full TeaVM JUnit suite remains a separate validation step.

API reference: [Java 21 AtomicReferenceArray](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/atomic/AtomicReferenceArray.html).
