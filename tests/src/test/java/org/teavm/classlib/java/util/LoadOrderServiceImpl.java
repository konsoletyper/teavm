/*
 *  Copyright 2020 Alexey Andreev.
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
package org.teavm.classlib.java.util;

public class LoadOrderServiceImpl implements LoadOrderService {
    static {
        Log.create();
    }

    public LoadOrderServiceImpl() {
        Log.run();
    }

    @Override
    public void run() {
        LoadOrderServiceLog.content.append("service run;");
    }

    static class Log {
        static {
            LoadOrderServiceLog.content.append("class init;");
        }

        static void create() {
            LoadOrderServiceLog.content.append("log create;");
        }

        static void run() {
            LoadOrderServiceLog.content.append("log run;");
        }
    }
}
