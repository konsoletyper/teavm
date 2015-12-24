#!/bin/sh

mvn versions:set -DnewVersion=$1 -DgenerateBackupPoms=false
pushd tools/eclipse
mvn tycho-versions:set-version -DnewVersion=$1
cd core-plugin
mvn -f dep-pom.xml versions:set -DnewVersion=$1 -DgenerateBackupPoms=false
popd