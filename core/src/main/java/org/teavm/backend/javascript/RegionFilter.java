/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.backend.javascript;

import org.teavm.backend.javascript.rendering.RenderingFilter;
import org.teavm.backend.javascript.splitting.RegionAnalyzer;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;

public class RegionFilter implements RenderingFilter {
    private RegionAnalyzer.Part currentPart;
    private RegionAnalyzer analyzer;

    public RegionFilter(RegionAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public void setCurrentPart(RegionAnalyzer.Part currentPart) {
        this.currentPart = currentPart;
    }

    @Override
    public boolean isExternal(MethodReference method) {
        RegionAnalyzer.NodeInfo info = analyzer.getNodeInfo(method);
        return info != null && info.getPart() != currentPart;
    }

    @Override
    public boolean isShared(MethodReference method) {
        RegionAnalyzer.NodeInfo info = analyzer.getNodeInfo(method);
        return info != null && info.isShared();
    }

    @Override
    public boolean isExternal(String className) {
        RegionAnalyzer.NodeInfo info = analyzer.getNodeInfo(className);
        return info != null && info.isShared();
    }

    @Override
    public boolean isShared(String className) {
        RegionAnalyzer.NodeInfo info = analyzer.getNodeInfo(className);
        return info != null && info.isShared();
    }

    @Override
    public boolean isExternal(FieldReference field) {
        RegionAnalyzer.NodeInfo info = analyzer.getNodeInfo(field);
        return info != null && info.getPart() != currentPart;
    }

    @Override
    public boolean isShared(FieldReference field) {
        RegionAnalyzer.NodeInfo info = analyzer.getNodeInfo(field);
        return info != null && info.isShared();
    }

    @Override
    public boolean isExternalString(String string) {
        RegionAnalyzer.NodeInfo info = analyzer.getNodeInfoByStringConstant(string);
        return info != null && info.getPart() != currentPart;
    }

    @Override
    public boolean isSharedString(String string) {
        RegionAnalyzer.NodeInfo info = analyzer.getNodeInfoByStringConstant(string);
        return info != null && info.isShared();
    }
}
