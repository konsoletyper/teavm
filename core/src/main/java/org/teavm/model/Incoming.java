/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.model;

public class Incoming implements IncomingReader {
    private Phi phi;
    private Variable value;
    private BasicBlock source;

    @Override
    public Variable getValue() {
        return value;
    }

    public void setValue(Variable value) {
        this.value = value;
    }

    @Override
    public BasicBlock getSource() {
        return source;
    }

    public void setSource(BasicBlock source) {
        this.source = source;
    }

    @Override
    public Phi getPhi() {
        return phi;
    }

    void setPhi(Phi phi) {
        this.phi = phi;
    }
}
