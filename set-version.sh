#!/bin/sh

mvn versions:set -DnewVersion=$1 -DgenerateBackupPoms=false
