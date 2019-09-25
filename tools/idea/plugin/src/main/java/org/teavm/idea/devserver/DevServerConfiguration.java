/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.idea.devserver;

public class DevServerConfiguration {
    public String javaHome;
    public int maxHeap;
    public String mainClass;
    public String[] classPath;
    public String[] sourcePath;
    public boolean indicator;
    public boolean deobfuscateStack;
    public boolean autoReload;
    public int port;
    public String pathToFile;
    public String fileName;
    public int debugPort;
    public String proxyUrl;
    public String proxyPath;
}
