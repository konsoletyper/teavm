#!/bin/sh
mkdir -p build
cd build
git clone https://github.com/unicode-cldr/cldr-core
git clone https://github.com/unicode-cldr/cldr-dates-modern.git
git clone https://github.com/unicode-cldr/cldr-numbers-modern.git
git clone https://github.com/unicode-cldr/cldr-localenames-modern.git
git clone https://github.com/unicode-cldr/cldr-misc-modern.git
mkdir -p result
mkdir -p result/supplemental
cp -f cldr-core/supplemental/weekData.json result/supplemental
cp -f cldr-core/supplemental/likelySubtags.json result/supplemental
cp -rf cldr-dates-modern/main/* result/
cp -rf cldr-numbers-modern/main/* result/
cp -rf cldr-localenames-modern/main/* result/
cp -rf cldr-misc-modern/main/* result/
cd result
zip -r ../cldr-json.zip *
cd ..
cp -f cldr-json.zip ../src/main/resources/org/teavm/classlib/impl/unicode/
cd ..
rm -rf build