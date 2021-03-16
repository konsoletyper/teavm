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

if [[ ! -d build-dir ]] ; then
  mkdir build-dir
fi

git archive HEAD | tar -x -C build-dir

pushd build-dir

mvn -e -V install \
 -P with-idea -P with-cli \
 -Dteavm.junit.optimized=false \
 -Dteavm.junit.js.decodeStack=false \
 -Dteavm.junit.threads=4 \
 -Dteavm.junit.js.runner=browser-chrome

rm -rf *

popd