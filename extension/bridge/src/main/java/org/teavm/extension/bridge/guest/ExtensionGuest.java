/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.extension.bridge.guest;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Predicate;
import org.teavm.extension.bridge.BridgeGuest;
import org.teavm.extension.bridge.BridgeHost;
import org.teavm.extension.bridge.BridgeResourceHost;
import org.teavm.extension.bridge.BridgeResourceTransformer;
import org.teavm.extension.bridge.BridgeSubstitutionRule;
import org.teavm.extension.spi.reflection.ReflectionPolicy;
import org.teavm.extension.spi.resources.ResourcesPolicy;
import org.teavm.extension.spi.substitution.ClassSubstitutionPolicy;
import org.teavm.extension.spi.substitution.SubstitutionPolicy;
import org.teavm.extension.spi.substitution.SubstitutionSink;
import org.teavm.extension.spi.transformation.ResourceTransformer;
import org.teavm.jso.JSExport;

/**
 * Entry point of guest module, i.e. module that contains extensions and is compiled by TeaVM.
 * Extensions are found via {@link ServiceLoader}, so the module contains all extensions registered
 * in the class path.
 *
 * @see org.teavm.extension.bridge
 */
public final class ExtensionGuest {
    private ExtensionGuest() {
    }

    @JSExport
    public static BridgeGuest create(BridgeHost host) {
        var env = new GuestEnvironment(host);
        return new GuestBridge(env, load(ReflectionPolicy.class), load(ResourcesPolicy.class));
    }

    @JSExport
    public static BridgeSubstitutionRule[] substitutionRules() {
        var rules = new ArrayList<GuestSubstitutionRule>();
        var sink = new SubstitutionSink() {
            @Override
            public ClassSubstitutionPolicy selectClasses(Predicate<String> predicate) {
                var rule = new GuestSubstitutionRule(predicate);
                rules.add(rule);
                return rule;
            }
        };
        for (var policy : load(SubstitutionPolicy.class)) {
            policy.contribute(sink);
        }
        return rules.toArray(new BridgeSubstitutionRule[0]);
    }

    @JSExport
    public static BridgeResourceTransformer createResourceTransformer(BridgeResourceHost host) {
        var transformers = load(ResourceTransformer.class);
        return !transformers.isEmpty() ? new GuestResourceTransformer(host, transformers) : null;
    }

    private static <T> List<T> load(Class<T> type) {
        var result = new ArrayList<T>();
        for (var provider : ServiceLoader.load(type)) {
            result.add(provider);
        }
        return result;
    }
}
