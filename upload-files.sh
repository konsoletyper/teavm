#!/bin/bash

echo "${SSH_KEY}" | base64 --decode >/tmp/sftp_rsa
chmod 0700 /tmp/sftp_rsa
SCP_TARGET="$FTP_USER@$FTP_HOST:~/$FTP_PATH"

#
# Upload CLI
#
scp -o StrictHostKeyChecking=no -i /tmp/sftp_rsa -B -r -P $FTP_PORT tools/cli/target/teavm-cli-$NEW_VERSION.jar $SCP_TARGET/cli/dev/

#
# Update IDEA repository descriptor
#
cat <<EOF >.idea-repository.xml
<?xml version="1.0" encoding="UTF-8"?>
<plugins>
  <plugin id="org.teavm.idea" url="https://dl.bintray.com/konsoletyper/teavm/org/teavm/teavm-idea/$NEW_VERSION/teavm-idea-$NEW_VERSION.zip" version="$NEW_VERSION">
    <idea-version since-build="173.*" until-build="223.*" />
    <description>TeaVM support</description>
  </plugin>
</plugins>
EOF

scp -o StrictHostKeyChecking=no -i /tmp/sftp_rsa -B -r -P $FTP_PORT .idea-repository.xml $SCP_TARGET/idea/dev/teavmRepository.xml

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