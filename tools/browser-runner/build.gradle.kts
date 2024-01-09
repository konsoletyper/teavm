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

description = "Runs JS tests in the browser"

configurations {
    create("js")
}

dependencies {
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.databind)
    implementation(libs.javax.servlet)
    implementation(libs.jetty.server)
    implementation(libs.jetty.websocket.server)
    implementation(libs.jetty.websocket.client)
    implementation(libs.jetty.websocket.client)

    "js"(project(":tools:deobfuscator-js", "js"))
}

tasks.withType<Jar>().configureEach {
    if (name == "relocateJar") {
        dependsOn(configurations["js"])
        from(project.provider { configurations["js"].map { zipTree(it) } }) {
            include("deobfuscator-lib.js")
            into("test-server")
            rename { "deobfuscator.js" }
        }
    }
}

teavmPublish {
    artifactId = "teavm-browser-runner"
}