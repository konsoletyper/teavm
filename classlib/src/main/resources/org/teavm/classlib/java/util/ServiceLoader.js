/*
 *  Copyright 2023 Alexey Andreev.
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

function loadServices(cls) {
    let serviceLoader = teavm_javaClass("java.util.ServiceLoader");
    if (!serviceLoader.$$services$$) {
        serviceLoader.$$services$$ = true;
        teavm_fragment("fillServices");
    }

    let serviceList = cls.$$serviceList$$;
    if (!serviceList) {
        return $rt_createArray($rt_objcls(), 0);
    }

    let result = $rt_createArray($rt_objcls(), serviceList.length);
    for (let i = 0; i < serviceList.length; ++i) {
        let serviceDesc = serviceList[i];
        let instance = new serviceDesc[0]();
        serviceDesc[1](instance);
        result.data[i] = instance;
    }

    return result;
}
