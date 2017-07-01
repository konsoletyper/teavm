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

public class TestResourceGenerator implements MetadataGenerator {
    @Override
    public Resource generateMetadata(MetadataGeneratorContext context, MethodReference method) {
        switch (method.getName()) {
            case "getNull":
                return null;
            case "getInt":
                return createInt(context, 23);
            case "getResource":
                return getResource(context);
            case "getEmptyResource":
                return context.createResource(TestResource.class);
            default:
                throw new RuntimeException("Unsupported method: " + method);
        }
    }

    private Resource getResource(MetadataGeneratorContext context) {
        TestResource resource = context.createResource(TestResource.class);
        resource.setA(23);
        resource.setB(false);
        resource.setD((byte) 24);
        resource.setE((short) 25);
        resource.setF(3.14f);
        resource.setG(2.72);
        resource.setFoo("qwe");

        ResourceArray<IntResource> array = context.createResourceArray();
        array.add(createInt(context, 2));
        array.add(createInt(context, 3));
        resource.setArrayA(array);
        DependentTestResource dep = context.createResource(DependentTestResource.class);
        dep.setBar("baz");
        ResourceArray<DependentTestResource> resArray = context.createResourceArray();
        resArray.add(dep);
        resource.setArrayB(resArray);
        return resource;
    }

    private IntResource createInt(MetadataGeneratorContext context, int value) {
        IntResource res = context.createResource(IntResource.class);
        res.setValue(value);
        return res;
    }
}
