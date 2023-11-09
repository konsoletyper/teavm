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
    scala
    war
    id("org.teavm")
}

dependencies {
    teavm(teavm.libs.jsoApis)
    teavm("org.scala-lang:scala-library:2.13.10")
}

teavm.js {
    addedToWebApp = true
    mainClass = "org.teavm.samples.scala.Client"
}

tasks.withType<ScalaCompile> {
    scalaCompileOptions.additionalParameters = listOf(
        "-feature",
        "-deprecation",
    )
}
