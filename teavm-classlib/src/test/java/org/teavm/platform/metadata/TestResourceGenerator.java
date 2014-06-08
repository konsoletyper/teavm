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

import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class TestResourceGenerator implements MetadataGenerator {
    @Override
    public Object generateMetadata(MetadataGeneratorContext context, MethodReference method) {
        switch (method.getName()) {
            case "getNull":
                return null;
            case "getInt":
                return 23;
            case "getResource":
                return getResource(context);
            default:
                throw new RuntimeException("Unsupported method: " + method);
        }
    }

    private Object getResource(MetadataGeneratorContext context) {
        TestResource resource = context.createResource(TestResource.class);
        resource.setA(23);
        resource.setB(false);
        resource.setD((byte)24);
        resource.setE((short)25);
        resource.setF(3.14f);
        resource.setG(2.72);
        resource.setH(26);
        resource.setI(null);
        resource.setJ((byte)27);
        resource.setK((short)28);
        resource.setL(100f);
        resource.setM(200.0);
        resource.setFoo("qwe");

        ResourceArray<Integer> array = context.createResourceArray();
        array.add(2);
        array.add(3);
        resource.setArrayA(array);
        DependentTestResource dep = context.createResource(DependentTestResource.class);
        dep.setBar("baz");
        ResourceArray<DependentTestResource> resArray = context.createResourceArray();
        resArray.add(dep);
        resource.setArrayB(resArray);
        return resource;
    }
}
