/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.model.optimization;

import java.util.HashMap;
import java.util.Map;
import org.teavm.model.InliningInfo;

class InliningInfoMerger {
    private InliningInfo parent;
    Map<InliningInfo, InliningInfo> inliningInfoCache = new HashMap<>();

    InliningInfoMerger(InliningInfo parent) {
        this.parent = parent;
    }

    InliningInfo merge(InliningInfo inliningInfo) {
        if (inliningInfo == null) {
            return parent;
        }

        InliningInfo result = inliningInfoCache.get(inliningInfo);
        if (result == null) {
            result = new InliningInfo(inliningInfo.getMethod(), inliningInfo.getFileName(), inliningInfo.getLine(),
                    merge(inliningInfo.getParent()));
            inliningInfoCache.put(inliningInfo, result);
        }
        return result;
    }
}
