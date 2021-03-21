#
#  Copyright 2021 Alexey Andreev.
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
mkdir -p build-cache/maven-repository

git fetch
git archive master | tar -x -C build-dir || { echo 'Git archive failed' ; exit 1; }

function deploy_teavm {
  TEAVM_OLD_VERSION=`mvn help:evaluate -Dexpression=project.version 2>/dev/null | grep -Ev "(^\[|Download)"`
  TEAVM_DEPLOY_VERSION=${TEAVM_OLD_VERSION%.0-SNAPSHOT}
  git rev-parse master >commit-id.txt
  TEAVM_DEPLOY_COMMIT_ID=`cat commit-id.txt`

  if [[ "200" == `curl --output response --silent --write-out "%{http_code}" https://teavm.org/maven/versions/$TEAVM_DEPLOY_VERSION.txt` ]] ; then
    TEAVM_DEPLOY_BUILD=$((`cat response` + 1))
  else
    TEAVM_DEPLOY_BUILD=1
  fi
  rm response

  TEAVM_DEPLOY_VERSION_FULL="$TEAVM_DEPLOY_VERSION.0-dev-$TEAVM_DEPLOY_BUILD"

  if [[ "200" == `curl --output response --silent --write-out "%{http_code}" https://teavm.org/maven/versions/$TEAVM_DEPLOY_VERSION_FULL-commit.txt` ]] ; then
    if [[ "$TEAVM_DEPLOY_COMMIT_ID" == `cat response` ]] ; then
      echo "There are no changes compared to previous build."
      exit 1
    fi
  fi
  rm response

  echo "Building version $TEAVM_DEPLOY_VERSION_FULL"

  sed -i -e "s/{{TEAVM_DEPLOY_URL}}/ftp:\/\/${TEAVM_DEPLOY_SERVER//\//\\/}\/maven\/repository/g" pom.xml

  mvn -e versions:set \
    -DnewVersion="$TEAVM_DEPLOY_VERSION_FULL" \
    -DgenerateBackupPoms=false \
    || { echo 'Setting version' ; return 1; }

  mvn -e -V deploy \
   --settings ../deploy-settings.xml \
   -P with-idea -P with-cli -P deploy-to-teavm \
   -Dmaven.repo.local=`pwd`/../build-cache/maven-repository \
   -Dteavm.build.all=false \
   -Dteavm.junit.optimized=false \
   -Dteavm.junit.js.decodeStack=false \
   -Dteavm.junit.threads=4 \
   -Dteavm.junit.js.runner=browser-chrome \
   || { echo 'Deploy failed' ; return 1; }

 cat <<EOF >idea-repository.xml
<?xml version="1.0" encoding="UTF-8"?>
<plugins>
  <plugin id="org.teavm.idea" url="https://teavm.org/maven/repository/org/teavm/teavm-idea/$TEAVM_DEPLOY_VERSION_FULL/teavm-idea-$TEAVM_DEPLOY_VERSION_FULL.zip" version="$TEAVM_DEPLOY_VERSION_FULL">
    <idea-version since-build="182.*" until-build="223.*" />
    <description>TeaVM support</description>
  </plugin>
</plugins>
EOF

  curl --output badge.svg "https://img.shields.io/static/v1?label=download&message=$TEAVM_DEPLOY_VERSION_FULL&color=green"
  cat <<EOF >htaccess
Redirect /maven/_latest /maven/repository/org/teavm/teavm-cli/$TEAVM_DEPLOY_VERSION_FULL/teavm-cli-$TEAVM_DEPLOY_VERSION_FULL.jar
EOF

  echo "$TEAVM_DEPLOY_BUILD" >build-number.txt

  return 0
}

pushd build-dir
deploy_teavm
EXIT_CODE=$?
if [[ "$EXIT_CODE" == '0' ]] ; then
  curl -T build-number.txt \
    ftp://$TEAVM_DEPLOY_SERVER/maven/versions/$TEAVM_DEPLOY_VERSION.txt \
    --user $TEAVM_DEPLOY_LOGIN:$TEAVM_DEPLOY_PASSWORD
  curl -T commit-id.txt \
    ftp://$TEAVM_DEPLOY_SERVER/maven/versions/$TEAVM_DEPLOY_VERSION_FULL-commit.txt \
    --user $TEAVM_DEPLOY_LOGIN:$TEAVM_DEPLOY_PASSWORD
  curl -T badge.svg \
    ftp://$TEAVM_DEPLOY_SERVER/maven/badge.svg \
    --user $TEAVM_DEPLOY_LOGIN:$TEAVM_DEPLOY_PASSWORD
  curl -T htaccess \
    ftp://$TEAVM_DEPLOY_SERVER/maven/.htaccess \
    --user $TEAVM_DEPLOY_LOGIN:$TEAVM_DEPLOY_PASSWORD
  curl -T idea-repository.xml \
    ftp://$TEAVM_DEPLOY_SERVER/idea/dev/teavmRepository.xml \
    --user $TEAVM_DEPLOY_LOGIN:$TEAVM_DEPLOY_PASSWORD
fi
popd

rm -rf build-dir
rm -rf build-cache/maven-repository/org/teavm
exit $EXIT_CODE
