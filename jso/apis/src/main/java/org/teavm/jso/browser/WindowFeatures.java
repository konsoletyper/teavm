/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.jso.browser;

public class WindowFeatures {
    StringBuilder sb = new StringBuilder();

    public WindowFeatures() {
    }

    public WindowFeatures left(int left) {
        return add("left=" + left);
    }

    public WindowFeatures top(int top) {
        return add("top=" + top);
    }

    public WindowFeatures width(int width) {
        return add("width=" + width);
    }

    public WindowFeatures height(int height) {
        return add("height=" + height);
    }

    public WindowFeatures menubar() {
        return add("menubar");
    }

    public WindowFeatures toolbar() {
        return add("toolbar");
    }

    public WindowFeatures location() {
        return add("location");
    }

    public WindowFeatures status() {
        return add("status");
    }

    public WindowFeatures resizable() {
        return add("resizable");
    }

    public WindowFeatures scrollbars() {
        return add("resizable");
    }

    private WindowFeatures add(String feature) {
        if (sb.length() > 0) {
            sb.append(',');
        }
        sb.append(feature);
        return this;
    }
}
