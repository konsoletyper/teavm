/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.platform.metadata;

public interface TestResource extends Resource {
    int getA();

    void setA(int a);

    boolean getB();

    void setB(boolean b);

    byte getD();

    void setD(byte d);

    short getE();

    void setE(short e);

    float getF();

    void setF(float f);

    double getG();

    void setG(double g);

    String getFoo();

    void setFoo(String foo);

    ResourceArray<IntResource> getArrayA();

    void setArrayA(ResourceArray<IntResource> arrayA);

    ResourceArray<DependentTestResource> getArrayB();

    void setArrayB(ResourceArray<DependentTestResource> arrayB);

    ResourceArray<ResourceArray<StringResource>> getArrayC();

    void setArrayC(ResourceArray<ResourceArray<StringResource>> arrayC);

    ResourceMap<IntResource> getMapA();

    void setMapA(ResourceMap<IntResource> mapA);

    ResourceMap<DependentTestResource> getMapB();

    void setMapB(ResourceMap<DependentTestResource> mapB);

    ResourceMap<ResourceArray<StringResource>> getMapC();

    void setMapC(ResourceMap<ResourceArray<StringResource>> mapC);
}
