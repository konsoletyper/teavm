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
package org.teavm.buildutil;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jreleaser.gradle.plugin.JReleaserExtension;
import org.jreleaser.gradle.plugin.JReleaserPlugin;
import org.jreleaser.model.Active;
import org.jreleaser.model.Signing;
import org.jreleaser.model.api.deploy.maven.MavenCentralMavenDeployer;

public class ReleaseTeaVMPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        var publish = Boolean.parseBoolean(target.getProviders().gradleProperty("teavm.mavenCentral.publish")
                .getOrElse("false"));
        if (publish) {
            target.getPlugins().apply(JReleaserPlugin.class);
        }
        if (publish) {
            target.afterEvaluate(p -> {
                var jreleaser = target.getExtensions().getByType(JReleaserExtension.class);
                jreleaser.getGitRootSearch().set(true);
                jreleaser.signing(signing -> {
                    var providers = target.getProviders();
                    signing.getActive().set(Active.ALWAYS);
                    signing.getArmored().set(true);
                    signing.getMode().set(Signing.Mode.COMMAND);
                    signing.command(command -> {
                        command.getKeyName().set(providers.gradleProperty("teavm.publish.gpg.keyName"));
                        command.getPublicKeyring().set(providers.gradleProperty(
                                "teavm.publish.gpg.secretKeyRingFile"));
                        command.getDefaultKeyring().set(true);
                        command.getArgs().add("--no-random-seed-file");
                    });
                    signing.getPassphrase().set(providers.gradleProperty("teavm.publish.gpg.password"));
                });
                jreleaser.deploy(deploy -> {
                    deploy.maven(maven -> {
                        maven.pomchecker(pomchecker -> {
                            pomchecker.getFailOnError().set(false);
                            pomchecker.getFailOnWarning().set(false);
                            pomchecker.getStrict().set(false);
                        });
                        maven.mavenCentral(mavenCentral -> {
                            var sonatype = maven.getMavenCentral().maybeCreate("sonatype");
                            sonatype.getActive().set(Active.ALWAYS);
                            sonatype.getUrl().set("https://central.sonatype.com/api/v1/publisher");
                            sonatype.getStagingRepositories().add("build/staging-deploy");
                            sonatype.getUsername().set(target.getProviders().gradleProperty("ossrhUsername"));
                            sonatype.getPassword().set(target.getProviders().gradleProperty("ossrhPassword"));
                            sonatype.getStage().set(MavenCentralMavenDeployer.Stage.FULL);
                            sonatype.getSign().set(true);
                            sonatype.getApplyMavenCentralRules().set(false);
                        });
                    });
                });
                jreleaser.release(release -> {
                    release.github(github -> {
                        github.getSkipRelease().set(true);
                        github.getToken().set("123");
                    });
                });
            });
        }
    }
}
