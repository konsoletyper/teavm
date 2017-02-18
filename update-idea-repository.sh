#!/bin/bash

cat <<EOF >.idea-repository.xml
<?xml version="1.0" encoding="UTF-8"?>
<plugins>
  <plugin id="org.teavm.idea" url="https://dl.bintray.com/konsoletyper/teavm/org/teavm/teavm-idea/$NEW_VERSION/:teavm-idea-$NEW_VERSION.zip" version="$NEW_VERSION">
    <idea-version since-build="163.12024.16" until-build="172.*" />
    <description>TeaVM support</description>
  </plugin>
</plugins>
EOF

curl --ftp-create-dirs -T .idea-repository.xml \
  -u $TEAVM_FTP_USER:$TEAVM_FTP_PASSWORD \
  ftp://$TEAVM_FTP_HOST/httpdocs/idea/dev/teavmRepository.xml
