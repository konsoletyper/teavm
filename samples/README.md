To build these samples, you should do one of:

* Publish TeaVM into local repository as described in [main readme](../README.md) (preferred way).
* Take `build.gradle.kts` and update TeaVM version to the most recent version, published in Maven Central. 
  Please, refer to [main readme](../README.md) for a badge with recent version number.

To quickly tests built war file, you can run `./gradlew :<example-name>:appRunWar` or 
`gradlew bat :<example-name>:appRunWar`.
Note that some examples also provide WASI and native binaries, please refer to corresponding build files.