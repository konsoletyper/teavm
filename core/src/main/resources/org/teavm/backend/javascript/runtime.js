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
"use strict";

let $rt_seed = 2463534242;
let $rt_nextId = () => {
    let x = $rt_seed;
    x ^= x << 13;
    x ^= x >>> 17;
    x ^= x << 5;
    $rt_seed = x;
    return x;
}

let $rt_wrapFunction0 = f => function() {
    return f(this);
}
let $rt_wrapFunction1 = f => function(p1) {
    return f(this, p1);
}
let $rt_wrapFunction2 = f => function (p1, p2) {
    return f(this, p1, p2);
}
let $rt_wrapFunction3 = f => function(p1, p2, p3) {
    return f(this, p1, p2, p3, p3);
}
let $rt_wrapFunction4 = f => function(p1, p2, p3, p4) {
    return f(this, p1, p2, p3, p4);
}
let $rt_threadStarter = f => function() {
    let args = teavm_globals.Array.prototype.slice.apply(arguments);
    $rt_startThread(function() {
        f.apply(this, args);
    });
}
let $rt_mainStarter = f => (args, callback) => {
    if (!args) {
        args = [];
    }
    let javaArgs = $rt_createArray($rt_objcls(), args.length);
    for (let i = 0; i < args.length; ++i) {
        javaArgs.data[i] = $rt_str(args[i]);
    }
    $rt_startThread(() => { f.call(null, javaArgs); }, callback);
}

let $rt_eraseClinit = target => target.$clinit = () => {};

let $dbg_class = obj => {
    let cls = obj.constructor;
    let arrayDegree = 0;
    while (cls.$meta && cls.$meta.item) {
        ++arrayDegree;
        cls = cls.$meta.item;
    }
    let clsName = "";
    if (cls.$meta.primitive) {
        clsName = cls.$meta.name;
    } else {
        clsName = cls.$meta ? (cls.$meta.name || ("a/" + cls.name)) : "@" + cls.name;
    }
    while (arrayDegree-- > 0) {
        clsName += "[]";
    }
    return clsName;
}

let $rt_classWithoutFields = superclass => {
    if (superclass === 0) {
        return function() {};
    }
    if (superclass === void 0) {
        superclass = $rt_objcls();
    }
    return function() {
        superclass.call(this);
    };
}


let $rt_cls = (cls) => teavm_javaMethod("java.lang.Class",
        "getClass(Lorg/teavm/platform/PlatformClass;)Ljava/lang/Class;")(cls);


let $rt_objcls = () => teavm_javaClass("java.lang.Object");

let $rt_getThread = () => {
    if (teavm_javaMethodExists("java.lang.Thread", "currentThread()Ljava/lang/Thread;")) {
        return teavm_javaMethod("java.lang.Thread", "currentThread()Ljava/lang/Thread;")();
    }
}
let $rt_setThread = t => {
    if (teavm_javaMethodExists("java.lang.Thread", "setCurrentThread(Ljava/lang/Thread;)V")) {
        return teavm_javaMethod("java.lang.Thread", "setCurrentThread(Ljava/lang/Thread;)V")(t);
    }
}
