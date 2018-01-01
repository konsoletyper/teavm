#!/bin/bash

curl --ftp-create-dirs -T tools/cli/target/teavm-cli-dev-$NEW_VERSION.jar \
  -u $TEAVM_FTP_LOGIN:$TEAVM_FTP_PASSWORD \
  ftp://$TEAVM_FTP_HOST/httpdocs/cli/dev/teavm-cli-dev-$NEW_VERSION.jar
