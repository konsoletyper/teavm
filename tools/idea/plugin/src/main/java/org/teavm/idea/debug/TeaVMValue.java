/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.idea.debug;

import com.intellij.util.PlatformIcons;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XNamedValue;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.teavm.common.Promise;
import org.teavm.debugging.Value;
import org.teavm.debugging.Variable;

public class TeaVMValue extends XNamedValue {
    private boolean root;
    private Value innerValue;

    public TeaVMValue(@NotNull String name, boolean root, Value innerValue) {
        super(name);
        this.root = root;
        this.innerValue = innerValue;
    }

    @Override
    public void computePresentation(@NotNull XValueNode node, @NotNull XValuePlace place) {
        Icon icon = root ? PlatformIcons.VARIABLE_ICON : PlatformIcons.FIELD_ICON;
        innerValue.getRepresentation()
                .then(representation -> representation != null ? representation : "null")
                .thenVoid(representation -> {
                    innerValue.getType().thenVoid(type -> {
                        if (Objects.equals(type, "java.lang.String")) {
                            getStringRepresentation().thenVoid(str -> node.setPresentation(icon, type, str, true));
                        } else {
                            node.setPresentation(icon, type, representation, innerValue.hasInnerStructure());
                        }
                    });
                });

    }

    private Promise<String> getStringRepresentation() {
        return innerValue.getProperties().thenAsync(properties -> {
            Variable charactersProperty = properties.get("characters");
            if (charactersProperty == null) {
                return errorString();
            }
            return charactersProperty.getValue().getProperties().thenAsync(dataValueProperties -> {
                int[] indexes = dataValueProperties.keySet().stream()
                        .filter(t -> isDigits(t))
                        .mapToInt(Integer::parseInt)
                        .toArray();
                int maxIndex = Math.min(Arrays.stream(indexes).max().orElse(-1) + 1, 256);
                char[] chars = new char[maxIndex];
                List<Promise<Void>> promises = new ArrayList<>();
                for (int i = 0; i < maxIndex; ++i) {
                    Variable charProperty = dataValueProperties.get(Integer.toString(i));
                    if (charProperty != null) {
                        int index = i;
                        promises.add(charProperty.getValue().getRepresentation().thenVoid(charRepr -> {
                            if (isDigits(charRepr)) {
                                chars[index] = (char) Integer.parseInt(charRepr);
                            }
                        }));
                    }
                }
                return Promise.allVoid(promises).thenAsync(v -> Promise.of(new String(chars)));
            });
        });
    }

    private Promise<String> errorString() {
        return Promise.of("<could not calculate string value>");
    }

    private static boolean isDigits(String str) {
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
        TeaVMStackFrame.computeChildrenImpl(node, innerValue.getProperties(), false);
    }
}
