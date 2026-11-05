# Base64 stream contracts

Run `gradle jvmContract checkstyleMain runJavaScriptContract -PclasslibJar=<fork-original-jar>`
after building `:classlib:jar`. The generated-JavaScript run replaces published 0.16 classlib
with the full fork. A JVM pass alone exercises the reference JDK, not the TeaVM implementation.

Tests cover 400 incremental basic/URL-safe padded/unpadded round trips (lengths 0 through 99),
small offset reads, flush versus close, close propagation/idempotence, closed stream errors,
malformed alphabet/padding, missing optional padding, trailing bytes after padding, and null
streams. URL-safe encoding includes a golden padded result because getUrlEncoder() must
include padding by default; callers can explicitly use withoutPadding().

These additions provide streaming basic and URL Base64 APIs. MIME stream variants are not
implemented or claimed. The runner requires a success marker and fails on errors/timeouts.
