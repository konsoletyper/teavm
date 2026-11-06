# Strong secure-random adapter contract

Build `:classlib:jar :jso:apis:relocateJar` in the TeaVM fork, then run:

```
gradle jvmContract checkstyleMain runJavaScriptContract runUnavailableContract runRealCryptoContract -PclasslibJar=<full-classlib-jar> -PjsoJar=<jso-apis-jar>
```

The class library and JSO API artifacts both replace their published counterparts. Copy the
artifacts into ignored build/inputs before parallel validation. A source composite may build
relocateJar without refreshing the separate original jar; use the actual verified artifact.

The production implementation uses Web Crypto getRandomValues, never a pseudorandom fallback.
The instrumented runner validates native calls and the 65,536-byte per-call limit; its fixed
bytes are test instrumentation only. The real runner uses Node's Web Crypto implementation.
The unavailable runner removes the crypto global and requires NoSuchAlgorithmException.
Tests include empty/large outputs, seed generation, null and negative arguments, and numeric
range contracts. These tests verify API plumbing, not statistical quality or certification.

Limit reference: https://www.w3.org/TR/webcrypto/#Crypto-method-getRandomValues
