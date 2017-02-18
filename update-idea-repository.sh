#!/bin/bash

cat <<EOF >.idea-repository.xml
<?xml version="1.0" encoding="UTF-8"?>
<plugins>
  <plugin id="org.teavm.idea" url="https://dl.bintray.com/konsoletyper/teavm/org/teavm/teavm-idea-plugin/$NEW_VERSION/teavm-idea-plugin-$NEW_VERSION.zip" version="$NEW_VERSION">
    <idea-version since-build="163.12024.16" until-build="172.*" />
    <description>TeaVM support</description>
  </plugin>
</plugins>
EOF

ftp -nv $TEAVM_FTP_HOST <<EOF
quot USER $TEAVM_FTP_LOGIN
quot PASS $TEAVM_FTP_PASSWORD
put .idea-repository.xml httpdocs/idea/dev/teavmRepository.xml
quit
EOF
