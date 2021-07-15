# Gradle plugin for TeaVM

Somewhat based on [the Kotlin version](https://github.com/edibleday/teavm-gradle-plugin), refactored and translated to Java for TeaVM code consistency.

## Using the plugin

Add build script dependency:
```
buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        classpath "org.teavm:teavm-gradle-plugin:0.4.3"
    }
}
```

Apply plugin:
```
apply plugin: "teavm"
```

Use `compileTeaVM` task to compile your application.

## Working with the source

Run `gradle eclipse` or `gradle idea` to generate IDE project.

### Publishing

Use `gradle build install` to publish the plugin into local Maven repository. If you want to publish to Maven Central, fill `gradle.properties` file with signing and logging data. (Be careful not to commit the changes! Hint: you can keep a separate `gradle.properties` file with your Maven Central data in your [Gradle home folder](http://mrhaki.blogspot.com/2015/10/gradle-goodness-setting-global.html).) Use `gradle uploadArchives` to publish the artifact. Changing the `isSnapshot` property to `-SNAPSHOT` or '' (empty string) allows to choose whether a snapshot or final version should be published.
