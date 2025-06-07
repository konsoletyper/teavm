import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

/*
 *  Copyright 2023 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

plugins {
    `java-library`
    `teavm-publish`
}

description = "Java class library emulation"

dependencies {
    implementation(project(":core"))

    api(project(":platform"))
    api(project(":jso:apis"))
    api(project(":jso:impl"))
    api(project(":metaprogramming:impl"))
    api(libs.commons.io)
    api(libs.jzlib)
    api(libs.jodaTime)

    testImplementation(libs.junit)
    testImplementation(project(":core"))
}

tasks {
    val generatedClassesDir = layout.buildDirectory.dir("generated/classes/java/main")
    val generateTzCache by registering(JavaExec::class) {
        val outputFile = generatedClassesDir.map { it.dir( "org/teavm/classlib/impl/tz/cache") }
        classpath(sourceSets.main.get().runtimeClasspath, sourceSets.main.get().compileClasspath)
        outputs.file(outputFile)
        inputs.files(sourceSets.main.get().runtimeClasspath)
        dependsOn(compileJava)
        mainClass = "org.teavm.classlib.impl.tz.TimeZoneCache"
        args(outputFile.get().asFile.absolutePath)
    }
    val generateIso4217 by registering(JavaExec::class) {
        val outputFile = generatedClassesDir.map { it.file("org/teavm/classlib/impl/currency/iso4217.bin") }
        val inputFile = layout.projectDirectory.file("src/main/data/iso4217.xml")
        classpath(sourceSets.main.get().runtimeClasspath, sourceSets.main.get().compileClasspath)
        outputs.file(outputFile)
        inputs.files(sourceSets.main.get().runtimeClasspath)
        inputs.file(inputFile)
        dependsOn(compileJava)
        mainClass = "org.teavm.classlib.impl.currency.CurrenciesGenerator"
        args(inputFile.asFile.absolutePath, outputFile.get().asFile.absolutePath)
    }
    jar {
        dependsOn(generateTzCache, generateIso4217)
        from(generatedClassesDir)
        exclude("html/**")
        exclude("org/teavm/classlib/impl/tz/tzdata*.zip")
    }
    sourcesJar {
        exclude("**/*.zip")
        exclude("**/UnicodeData.txt")
        exclude("**/iso*.xml")
        exclude("**/iso*.csv")
    }
    withType<ShadowJar> {
        dependsOn(generateTzCache, generateIso4217)
        from(generatedClassesDir)
        exclude("html/**")
        exclude("org/teavm/classlib/impl/tz/tzdata*.zip")
    }
}

teavmPublish {
    artifactId = "teavm-classlib"
}

