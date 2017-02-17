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
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
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
        node.setPresentation(icon, innerValue.getType(), innerValue.getRepresentation(),
                !innerValue.getProperties().isEmpty());
    }

    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
        XValueChildrenList children = new XValueChildrenList();
        for (Variable variable : innerValue.getProperties().values()) {
            children.add(new TeaVMValue(variable.getName(), true, variable.getValue()));
        }
        node.addChildren(children, true);
    }
}
