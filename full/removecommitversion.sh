#!/bin/sh

# Move original file back

MANIFEST=AndroidManifest.xml
ORGMANIFEST=.orgmanifest.xml
TMPMANIFEST=.tmpmanifest.xml

# Remove commit version again
# Get current commit
COMMIT=$(git rev-parse --short HEAD)
cat $MANIFEST | sed -r "s/-$COMMIT//" > $ORGMANIFEST

rm $MANIFEST
mv $ORGMANIFEST $MANIFEST
