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
package org.teavm.compiler.bootstrap.extension;

import java.util.List;
import org.teavm.extension.bridge.BridgeGuestModule;
import org.teavm.extension.bridge.BridgeSubstitutionRule;
import org.teavm.extension.spi.substitution.SubstitutionPolicy;
import org.teavm.extension.spi.substitution.SubstitutionSink;

/**
 * Replays rules, contributed by {@link SubstitutionPolicy}s, that run in guest module.
 * Predicates of rules are evaluated by guest.
 */
public class GuestSubstitutionPolicy implements SubstitutionPolicy {
    private final List<Rule> rules;

    private GuestSubstitutionPolicy(List<Rule> rules) {
        this.rules = rules;
    }

    /**
     * Collects rules from guest module.
     *
     * @return policy or {@code null} if guest module does not contain any substitution policies.
     */
    public static GuestSubstitutionPolicy create(BridgeGuestModule module) {
        var remoteRules = module.substitutionRules();
        if (remoteRules.length == 0) {
            return null;
        }
        var rules = new Rule[remoteRules.length];
        for (var i = 0; i < remoteRules.length; ++i) {
            rules[i] = new Rule(remoteRules[i]);
        }
        return new GuestSubstitutionPolicy(List.of(rules));
    }

    @Override
    public void contribute(SubstitutionSink sink) {
        for (var rule : rules) {
            var remote = rule.remote;
            var policy = sink.selectClasses(remote::matches);
            if (rule.replacedPackage != null) {
                policy.replacePackage(rule.replacedPackage, rule.packagePrefix);
            } else {
                policy.packagePrefix(rule.packagePrefix);
            }
            policy.packageSuffix(rule.packageSuffix);
            policy.simpleNamePrefix(rule.simpleNamePrefix);
            policy.simpleNameSuffix(rule.simpleNameSuffix);
            if (rule.dontFallback) {
                policy.dontFallbackWhenNoSubstitution();
            }
        }
    }

    private static class Rule {
        final BridgeSubstitutionRule remote;
        final String simpleNamePrefix;
        final String simpleNameSuffix;
        final String packagePrefix;
        final String packageSuffix;
        final String replacedPackage;
        final boolean dontFallback;

        Rule(BridgeSubstitutionRule remote) {
            this.remote = remote;
            simpleNamePrefix = remote.simpleNamePrefix();
            simpleNameSuffix = remote.simpleNameSuffix();
            packagePrefix = remote.packagePrefix();
            packageSuffix = remote.packageSuffix();
            replacedPackage = remote.replacedPackage();
            dontFallback = remote.dontFallback();
        }
    }
}
