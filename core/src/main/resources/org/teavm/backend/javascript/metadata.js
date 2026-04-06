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
"use strict";

let $rt_packageData = null;
let $rt_packages = data => {
    let i = 0;
    let packages = new teavm_globals.Array(data.length);
    for (let j = 0; j < data.length; ++j) {
        let prefixIndex = data[i++];
        let prefix = prefixIndex >= 0 ? packages[prefixIndex] : "";
        packages[j] = prefix + data[i++] + ".";
    }
    $rt_packageData = packages;
}
let $rt_allClasses = [];
let $rt_allClassesPointer = 0;
let $rt_allClassesRewind = () => $rt_allClassesPointer = 0;
let $rt_allClassesHasNext = () => $rt_allClassesPointer < $rt_allClasses.length;
let $rt_allClassesNext = () => $rt_allClasses[$rt_allClassesPointer++];
let $rt_metadata = data => {
    let packages = $rt_packageData;
    let i = 0;
    while (i < data.length) {
        let cls = data[i++];
        $rt_allClasses.push(cls);
        let m = $rt_newClassMetadata();
        cls[$rt_meta] = m;
        let className = data[i++];

        m.name = className !== 0 ? className : null;
        if (m.name !== null) {
            let packageIndex = data[i++];
            if (packageIndex >= 0) {
                m.name = packages[packageIndex] + m.name;
            }
        }

        m.binaryName = "L" + m.name + ";";
        let superclass = data[i++];
        m.parent = superclass !== 0 ? superclass : null;
        m.superinterfaces = data[i++];
        if (m.parent) {
            cls.prototype = teavm_globals.Object.create(m.parent.prototype);
        } else {
            cls.prototype = {};
        }
        cls.prototype.constructor = cls;
        m.modifiers = data[i++];
        m.primitiveKind = 0;

        let innerClassInfo = data[i++];
        if (innerClassInfo !== 0) {
            let enclosingClass = innerClassInfo[0];
            m.enclosingClass = enclosingClass !== 0 ? enclosingClass : null;
            let declaringClass = innerClassInfo[1];
            m.declaringClass = declaringClass !== 0 ? declaringClass : null;
            let simpleName = innerClassInfo[2];
            m.simpleName = simpleName !== 0 ? simpleName : null;
        }

        let clinit = data[i++];
        m.clinit = clinit !== 0
            ? () => { m.clinit = () => {}; clinit(); }
            : () => {};

        let virtualMethods = data[i++];
        if (virtualMethods !== 0) {
            for (let j = 0; j < virtualMethods.length; j += 2) {
                let name = virtualMethods[j];
                let func = virtualMethods[j + 1];
                if (typeof name === 'string') {
                    name = [name];
                }
                for (let k = 0; k < name.length; ++k) {
                    cls.prototype[name[k]] = func;
                }
            }
        }
    }
};
let $rt_enumConstantsMetadata = data => {
    let i = 0;
    while (i < data.length) {
        let cls = data[i++];
        cls[$rt_meta].enumConstants = data[i++];
    }
};
let $rt_classAnnotationMetadata = data => {
    let i = 0;
    while (i < data.length) {
        let cls = data[i++];
        $rt_classReflectionMetadata(cls).annotations = $rt_readAnnotations(data[i++]);
    }
};
let $rt_simpleConstructors = data => {
    let i = 0;
    while (i < data.length) {
        let cls = data[i++];
        cls[$rt_meta].constructor = data[i++];
    }
};
let $rt_reflection = data => {
    let i = 0;
    while (i < data.length) {
        let cls = data[i++];
        let clsData = $rt_classReflectionMetadata(cls);
        let obj = data[i++];

        if (teavm_javaMethodExists("org.teavm.runtime.reflect.ClassReflectionInfo", "typeParameterCount()I")) {
            let resolvedTypeParameters;
            let typeParameters = obj.p;
            if (typeof typeParameters !== 'undefined') {
                resolvedTypeParameters = new Array(typeParameters.length);
                for (let j = 0; j < typeParameters.length; ++j) {
                    resolvedTypeParameters[j] = $rt_readTypeParameter(typeParameters[j]);
                }
            } else {
                resolvedTypeParameters = [];
            }
            clsData.typeParameters = resolvedTypeParameters;
        }

        if (teavm_javaMethodExists("org.teavm.runtime.reflect.ClassReflectionInfo", "fieldCount()I")) {
            let resolvedFields;
            let fields = obj.f;
            if (typeof fields !== "undefined") {
                resolvedFields = new Array(fields.length);
                for (let j = 0; j < fields.length; ++j) {
                    resolvedFields[j] = $rt_readFieldMetadata(cls, fields[j]);
                }
            } else {
                resolvedFields = [];
            }
            clsData.fields = resolvedFields;
        }

        if (teavm_javaMethodExists("org.teavm.runtime.reflect.ClassReflectionInfo", "methodCount()I")) {
            let resolvedMethods;
            let methods = obj.m;
            if (typeof methods !== "undefined") {
                resolvedMethods = new Array(methods.length);
                for (let j = 0; j < methods.length; ++j) {
                    resolvedMethods[j] = $rt_readMethodMetadata(cls, methods[j]);
                }
            } else {
                resolvedMethods = [];
            }
            clsData.methods = resolvedMethods;
        }
        if (teavm_javaMethodExists("org.teavm.runtime.reflect.ClassReflectionInfo", "innerClassCount()I")) {
            let innerClasses = obj.c;
            if (typeof innerClasses !== "undefined") {
                clsData.innerClasses = innerClasses;
            }
        }
    }
};
let $rt_readTypeParameter = typeParam => {
    let bounds = typeParam.length > 1 ? typeParam[1] : [];
    let resolvedBounds = new Array(bounds.length);
    for (let i = 0; i < bounds.length; ++i) {
        resolvedBounds[i] = $rt_readGenericType(bounds[i]);
    }
    return {
        name: typeParam[0],
        bounds: resolvedBounds
    }
};
let $rt_readFieldMetadata = (cls, field) => {
    let writer = field[4];
    let fieldReflection = field[5];
    let resolvedFieldReflection;
    if (fieldReflection !== void 0) {
        resolvedFieldReflection = {
            annotations: fieldReflection.a !== void 0 ? $rt_readAnnotations(fieldReflection.a) : [],
            genericType: fieldReflection.t !== void 0 ? $rt_readGenericType(fieldReflection.t) : null
        }
    } else {
        resolvedFieldReflection = null;
    }
    return {
        cls: cls,
        name: field[0],
        modifiers: field[1],
        type: field[2],
        reader: field[3],
        writer: writer !== 0 ? writer : null,
        reflection: resolvedFieldReflection
    };
};
let $rt_readMethodMetadata = (cls, method) => {
    let methodReflection = method[5];
    let resolvedMethodReflection;
    if (methodReflection !== void 0) {
        let resolvedGenericParameterTypes;
        let parameterTypes = methodReflection.s;
        if (typeof parameterTypes !== "undefined") {
            resolvedGenericParameterTypes = new Array(parameterTypes.length);
            for (let i = 0; i < parameterTypes.length; ++i) {
                resolvedGenericParameterTypes[i] = $rt_readGenericType(parameterTypes[i]);
            }
        } else {
            resolvedGenericParameterTypes = null;
        }
        let resolvedTypeParameters;
        let typeParameters = methodReflection.p;
        if (typeof typeParameters !== "undefined") {
            resolvedTypeParameters = new Array(typeParameters.length);
            for (let i = 0; i < typeParameters.length; ++i) {
                resolvedTypeParameters[i] = $rt_readTypeParameter(typeParameters[i]);
            }
        } else {
            resolvedTypeParameters = [];
        }
        resolvedMethodReflection = {
            annotations: methodReflection.a !== void 0 ? $rt_readAnnotations(methodReflection.a) : [],
            genericReturnType: methodReflection.r !== void 0 ? $rt_readGenericType(methodReflection.r) : null,
            genericParameterTypes: resolvedGenericParameterTypes,
            typeParameters: resolvedTypeParameters
        }
    } else {
        resolvedMethodReflection = null;
    }
    let parameterTypes = method[3];
    if (parameterTypes === 0) {
        parameterTypes = [];
    }
    let caller = method[4];
    if (caller === 0) {
        caller = null;
    }
    let modifiers = method[1];
    let name = method[0];
    return {
        cls: cls,
        name: name,
        modifiers: modifiers,
        returnType: method[2],
        parameterTypes: parameterTypes,
        caller: caller,
        calledDirectly: (modifiers & 8) === 0 && ((modifiers & 2) !== 0 || name === '<init>'),
        reflection: resolvedMethodReflection
    };
};
let $rt_readAnnotations = annotations => {
    let resolvedAnnotations = new Array(annotations.length);
    for (let j = 0; j < annotations.length; ++j) {
        resolvedAnnotations[j] = $rt_readAnnotation(annotations[j]);
    }
    return resolvedAnnotations;
}
let $rt_readAnnotation = annotation => {
    return [annotation[0], annotation.slice(1)];
};
let $rt_readGenericType = data => {
    let kind = data[0];
    switch (kind) {
        case 0: {
            let typeArgsData = data.length > 2 ? data[2] : [];
            let typeArguments = new Array(typeArgsData.length);
            for (let i = 0; i < typeArgsData.length; ++i) {
                typeArguments[i] = $rt_readGenericType(typeArgsData[i]);
            }
            return {
                kind: 0,
                rawType: data[1],
                actualTypeArguments: typeArguments,
                ownerType: data.length > 3 ? $rt_readGenericType(data[3]) : null
            };
        }
        case 1:
            return {
                kind: 1,
                index: data[1],
                level: data.length > 2 ? data[2] : 0
            };
        case 2:
            return {
                kind: 2,
                itemType: $rt_readGenericType(data[1])
            };
        case 3:
        case 4:
            return {
                kind: kind,
                bound: $rt_readGenericType(data[1])
            };
        case 5:
            return { kind: 5 };
        case 6:
            return { kind: 6, rawType: data[1] };
    }
};