#
#  Copyright 2023 Alexey Andreev.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

mkdir -p build-dir

git fetch
git archive master | tar -x -C build-dir || { echo 'Git archive failed' ; exit 1; }

TEAVM_RELEASE_VERSION=$1

function release_teavm {
  echo "Building version $TEAVM_RELEASE_VERSION"

  GRADLE="./gradlew"
  GRADLE+=" --no-daemon --no-configuration-cache --stacktrace"
  GRADLE+=" -Pteavm.mavenCentral.publish=true"
  GRADLE+=" -Pteavm.project.version=$TEAVM_RELEASE_VERSION"
  GRADLE+=" -Psigning.keyId=$TEAVM_GPG_KEY_ID"
  GRADLE+=" -Psigning.password=$TEAVM_GPG_PASSWORD"
  GRADLE+=" -Psigning.secretKeyRingFile=$HOME/.gnupg/secring.gpg"
  GRADLE+=" -PossrhUsername=$TEAVM_SONATYPE_LOGIN"
  GRADLE+=" -PossrhPassword=$TEAVM_SONATYPE_PASSWORD"
  GRADLE+=" -Pteavm.idea.publishToken=$TEAVM_INTELLIJ_TOKEN"

  $GRADLE build -x test || { echo 'Build failed' ; return 1; }
  $GRADLE --max-workers 1 publish publishPlugin publishPlugins || { echo 'Release failed' ; return 1; }

  return 0
}

pushd build-dir
release_teavm
EXIT_CODE=$?
popd
rm -rf build-dir
exit $EXIT_CODE
