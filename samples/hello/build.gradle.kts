import org.teavm.gradle.api.SourceFilePolicy

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
    java
    war
    id("org.teavm")
}

configurations {
    create("teavmCli")
    create("teavmClasslib")
}

dependencies {
    teavm(teavm.libs.jsoApis)
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")

    "teavmCli"("org.teavm:teavm-cli:0.10.0-SNAPSHOT")
    "teavmClasslib"("org.teavm:teavm-classlib:0.10.0-SNAPSHOT")
}

teavm.js {
    addedToWebApp = true
    mainClass = "org.teavm.samples.hello.Client"
    sourceMap = true
    sourceFilePolicy = SourceFilePolicy.LINK_LOCAL_FILES
}

tasks.register<JavaExec>("runCli") {
    classpath(configurations["teavmCli"])
    mainClass = "org.teavm.cli.devserver.TeaVMDevServerRunner"
    args = listOf("--json-interface", "--no-watch", "-p",
        layout.buildDirectory.dir("classes/java/teavm").get().asFile.absolutePath,
    ) + configurations["teavmClasslib"].flatMap { listOf("-p", it.absolutePath) } + listOf(
        "--", "org.teavm.samples.hello.Client"
    )
    println(args)
}