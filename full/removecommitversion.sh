#!/bin/sh

# Move original file back

MANIFEST=AndroidManifest.xml
ORGMANIFEST=.orgmanifest.xml
TMPMANIFEST=.tmpmanifest.xml

rm $MANIFEST
mv $ORGMANIFEST $MANIFEST

# Remove commit version again
# Get current commit
#COMMIT=$(git rev-parse --short HEAD)
#cat $MANIFEST | sed -r "s/-$COMMIT//" > $MANIFEST
