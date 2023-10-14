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

description = "Set of JavaScript API wrappers for JSO"

configurations {
    val teavm = create("teavm")
    teavm.extendsFrom(compileClasspath.get())
}

dependencies {
    api(project(":jso:core"))
    api(project(":interop:core"))
}

teavmPublish {
    artifactId = "teavm-jso-apis"
}

tasks.withType<Jar> {
    manifest {
        attributes["Automatic-Module-Name"] = "org.teavm.jso.apis"
    }
}
