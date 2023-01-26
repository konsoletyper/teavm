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

description = "Development server that builds and serves JavaScript files"

configurations {
    create("js")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":tools:core"))
    implementation(libs.jetty.server)
    implementation(libs.jetty.websocket.server)
    implementation(libs.jetty.websocket.client)
    implementation(libs.javax.servlet)
    implementation(libs.jackson.databind)
    implementation(libs.httpclient)
    implementation(libs.commons.io)

    "js"(project(":tools:deobfuscator-js", "js"))
}

tasks.withType<Jar>().configureEach {
    if (name == "relocateJar") {
        dependsOn(configurations["js"])
        from(provider { configurations["js"].map { zipTree(it) } }) {
            include("deobfuscator.js")
            into("teavm/devserver")
        }
    }
}

teavmPublish {
    artifactId = "teavm-devserver"
}