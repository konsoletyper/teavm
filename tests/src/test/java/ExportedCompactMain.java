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

// Compact source file (JEP 512), must be in the unnamed package
import org.teavm.jso.JSExport;

static String result = "";

int value = 40;

int answer() {
    return value + 2;
}

void main() {
    result = String.valueOf(answer());
}

@JSExport
static String log() {
    return result;
}
