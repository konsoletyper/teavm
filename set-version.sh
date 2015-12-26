#!/bin/sh

mvn versions:set -DnewVersion=$1 -DgenerateBackupPoms=false
pushd tools/eclipse
mvn org.eclipse.tycho:tycho-versions-plugin:0.21.0:set-version -DnewVersion=$1

pushd core-plugin
sed -r -i -e "s/<version><!-- update -->(.+)<\/version>/<version><!-- update -->$1<\/version>/" dep-pom.xml
sed -r -i -e "s/(lib\/teavm(-[a-z]+)+)-.+\.jar/\1-$1.jar/" build.properties
sed -r -i -e "s/(lib\/teavm(-[a-z]+)+)-.+\.jar/\1-$1.jar/" META-INF/MANIFEST.MF
popd

#pushd m2e-plugin
#sed -r -i -e "s/<versionRange>.+<\/versionRange>/<versionRange>$1<\/versionRange>/" lifecycle-mapping-metadata.xml
#popd
popd
