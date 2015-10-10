/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.tooling;

/**
 *
 * @author Alexey Andreev
 */
public interface TeaVMToolLog {
    void info(String text);

    void debug(String text);

    void warning(String text);

    void error(String text);

    void info(String text, Throwable e);

    void debug(String text, Throwable e);

    void warning(String text, Throwable e);

    void error(String text, Throwable e);
}
