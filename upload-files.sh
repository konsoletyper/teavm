#!/bin/bash

#
# Upload CLI
#
curl -v --ftp-create-dirs -T tools/cli/target/teavm-cli-$NEW_VERSION.jar \
  -u $TEAVM_FTP_LOGIN:$TEAVM_FTP_PASSWORD \
  ftp://$TEAVM_FTP_HOST/httpdocs/cli/dev/teavm-cli-$NEW_VERSION.jar


#
# Update IDEA repository descriptor
#
cat <<EOF >.idea-repository.xml
<?xml version="1.0" encoding="UTF-8"?>
<plugins>
  <plugin id="org.teavm.idea" url="https://dl.bintray.com/konsoletyper/teavm/org/teavm/teavm-idea/$NEW_VERSION/teavm-idea-$NEW_VERSION.zip" version="$NEW_VERSION">
    <idea-version since-build="173.*" until-build="193.*" />
    <description>TeaVM support</description>
  </plugin>
</plugins>
EOF

curl -v --ftp-create-dirs -T .idea-repository.xml \
  -u $TEAVM_FTP_LOGIN:$TEAVM_FTP_PASSWORD \
  ftp://$TEAVM_FTP_HOST/httpdocs/idea/dev/teavmRepository.xml


#
# Upload Eclipse plugin
#
#cd tools/eclipse/updatesite/target/repository
#  find . -type f -exec curl \
#    --ftp-create-dirs \
#    -u $TEAVM_FTP_LOGIN:$TEAVM_FTP_PASSWORD \
#    -T {} \
#    ftp://$TEAVM_FTP_HOST/httpdocs/eclipse/update-site/$BASE_VERSION-dev/{} \;
#cd ../../../../..