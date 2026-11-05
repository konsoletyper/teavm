plugins {
    java
    checkstyle
    id("org.teavm") version "0.16.0"
}

repositories { mavenCentral() }
dependencies { teavm(teavm.libs.jsoApis) }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }

sourceSets.main {
    java.srcDir("../classlib/src/main/java")
    java.include("repro/**", "org/teavm/classlib/java/util/concurrent/atomic/TAtomicReferenceArray.java")
}

checkstyle {
    toolVersion = "10.21.0"
    configDirectory.set(layout.projectDirectory.dir("../config/checkstyle"))
}

teavm.js {
    mainClass = "repro.AtomicArrayContract"
}

tasks.register<JavaExec>("jvmContract") {
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("repro.AtomicArrayContract")
}
