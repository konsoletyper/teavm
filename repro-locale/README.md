# Locale parsing and collation contract

This fixture compares TeaVM 0.16's locale parsing and sorting behavior with
the JDK reference, and the generated JavaScript run checks its expected ordering
against Node's native `Intl.Collator` for German, English, and Turkish.

The cases cover BCP 47 language/script/region/variant fields, Unicode and
private-use extensions, preferred extlang handling, malformed-tag valid-prefix
behavior, and the locale-sensitive comparisons used by Unciv's UI sorting.
Host ICU/CLDR versions can change collation details, so the contract asserts
stable comparisons and documents that generated JavaScript follows the host's
`Intl.Collator` data rather than claiming byte-for-byte equality with every JDK.

From the TeaVM checkout root, build the fork classlib and run:

```powershell
gradle :classlib:jar
gradle -p repro-locale clean jvmContract checkstyleMain runJavaScriptContract
```

For an immutable fork artifact, pass `-PclasslibJar=<absolute-fork-classlib.jar>`.
