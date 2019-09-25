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
package org.teavm.model;

public class Outgoing {
    private Sigma sigma;
    private Variable value;
    private BasicBlock target;

    public Outgoing(Variable value, BasicBlock target) {
        this.value = value;
        this.target = target;
    }

    public Sigma getSigma() {
        return sigma;
    }

    void setSigma(Sigma sigma) {
        this.sigma = sigma;
    }

    public Variable getValue() {
        return value;
    }

    public void setValue(Variable value) {
        this.value = value;
    }

    public BasicBlock getTarget() {
        return target;
    }

    public void setTarget(BasicBlock target) {
        this.target = target;
    }
}
