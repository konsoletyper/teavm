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
    shadowApply
}

description = "All-in one JAR file that used by IDE plugins"

dependencies {
    implementation(project(path = ":tools:core"))
    implementation(project(path = ":tools:devserver"))
    implementation(project(path = ":classlib"))
    implementation(project(path = ":tools:chrome-rdp"))
}

tasks.shadowJar {
    archiveFileName = "teavm.jar"
    mergeServiceFiles()
}