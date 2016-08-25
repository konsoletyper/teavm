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
package org.teavm.backend.wasm.render;

import java.util.ArrayList;
import java.util.List;
import org.teavm.model.TextLocation;

class CExpression {
    private String text;
    private boolean relocatable;
    private List<CLine> lines = new ArrayList<>();

    public CExpression(String text) {
        this.text = text;
    }

    public CExpression() {
    }

    public boolean isRelocatable() {
        return relocatable;
    }

    public void setRelocatable(boolean relocatable) {
        this.relocatable = relocatable;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<CLine> getLines() {
        return lines;
    }

    public void addLine(String text, TextLocation location) {
        lines.add(new CSingleLine(text, location));
    }

    public void addLine(String text) {
        addLine(text, null);
    }

    public static CExpression relocatable(String text) {
        CExpression expression = new CExpression(text);
        expression.setRelocatable(true);
        return expression;
    }
}
