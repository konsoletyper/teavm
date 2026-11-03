/*
 *  Copyright 2026 Alexey Andreev.
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

description = "Protocol that allows to run TeaVM extensions (like reflection policies) in a separate module, " +
        "compiled by TeaVM, and the guest side implementation of this protocol"

dependencies {
    api(project(":jso:core"))
    api(project(":extension:spi"))
    api(project(":jso:apis"))
}

teavmPublish {
    artifactId = "teavm-extension-bridge"
}

javaVersion {
    version = JavaVersion.VERSION_11
}
