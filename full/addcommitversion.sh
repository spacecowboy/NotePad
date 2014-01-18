#!/bin/sh

# Get current commit
COMMIT=$(git rev-parse --short HEAD)

MANIFEST=AndroidManifest.xml
ORGMANIFEST=.orgmanifest.xml
TMPMANIFEST=.tmpmanifest.xml

# Add the commit version to manifest
cat $MANIFEST | sed -r "s/(versionName)=\"(.+?)\"/\1=\"\2-$COMMIT\"/" > $TMPMANIFEST

# Move files around
mv $MANIFEST $ORGMANIFEST
mv $TMPMANIFEST $MANIFEST
