plugins {
    java
    id("org.teavm") version "0.15.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.teavm:teavm-classlib:0.15.0")
    implementation("org.teavm:teavm-core:0.15.0")
    implementation("org.teavm:teavm-platform:0.15.0")
    implementation("org.teavm:teavm-interop:0.15.0")
    teavm(teavm.libs.jsoApis)
}

sourceSets.main {
    java.srcDir("../classlib/src/main/java")
    java.include("repro/**/*.java")
    java.include("org/teavm/classlib/java/lang/TThreadSupport.java")
    java.include("org/teavm/classlib/java/util/concurrent/TThreadFactory.java")
    java.include("org/teavm/classlib/java/util/concurrent/TFuture.java")
    java.include("org/teavm/classlib/java/util/concurrent/TExecutorService.java")
    java.include("org/teavm/classlib/java/util/concurrent/TRejectedExecutionException.java")
    java.include("org/teavm/classlib/java/util/concurrent/TTimeoutException.java")
    java.include("org/teavm/classlib/java/util/concurrent/TFutureTask.java")
    java.include("org/teavm/classlib/java/util/concurrent/TExecutors.java")
    java.include("org/teavm/classlib/java/util/concurrent/locks/TLockSupport.java")
    java.include("org/teavm/classlib/java/util/concurrent/atomic/TAtomicReferenceArray.java")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

teavm.js {
    mainClass = "repro.ExecutorServiceRepro"
}

tasks.register<Jar>("teavmCompatibilityJar") {
    dependsOn(tasks.classes)
    archiveFileName.set("teavm-compatibility-0.15.0.jar")
    destinationDirectory.set(layout.buildDirectory.dir("compat"))
    from(sourceSets.named("main").get().output) {
        include("org/teavm/classlib/**")
    }
}

tasks.register<Exec>("runJavaScriptRepro") {
    dependsOn("generateJavaScript")
    workingDir = projectDir
    commandLine("node", "run.cjs")
}
