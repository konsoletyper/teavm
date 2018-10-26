#!/bin/sh

$MVN_CMD versions:set -DnewVersion=$1 -DgenerateBackupPoms=false

cd tools/eclipse
$MVN_CMD tycho-versions:set-version -DnewVersion=$2

cd core-plugin
sed -r -i -e "s/<version><!-- update -->(.+)<\/version>/<version><!-- update -->$1<\/version>/" dep-pom.xml
sed -r -i -e "s/(lib\/teavm(-[a-z]+)+)-.+\.jar/\1-$1.jar/" build.properties
sed -r -i -e "s/(lib\/teavm(-[a-z]+)+)-.+\.jar/\1-$1.jar/" META-INF/MANIFEST.MF
cd ..

cd m2e-plugin
sed -r -i -e "s/<versionRange>.+<\/versionRange>/<versionRange>$1<\/versionRange>/" lifecycle-mapping-metadata.xml
cd ..

cd ../..

