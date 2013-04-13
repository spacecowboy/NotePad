#!/bin/bash

#Edit these bits
SDKDIR=~/android-sdk-linux
BASEDIR=/home/jonas/workspace/NotePad

# These should remain fixed
UPDATELIB="$SDKDIR/tools/android update lib-project"
UPDATEPROJ="$SDKDIR/tools/android update project"
TARGET="-t android-17"
VIEWPAGERDIR="Android-ViewPagerIndicator/library"
BETTERPICKDIR="android-betterpickers/library"
DRAGSORTLIST="drag-sort-listview/library"
WIZARD="wizardpager"
UTILS="Utils"

NOTES="NoNonsenseNotes"

# The following is to make it all buildable with ant

# Viewpager
$UPDATELIB -p $BASEDIR/$VIEWPAGERDIR $TARGET
#cd Android-ViewPagerIndicator/library;ant debug

# Better pickers
$UPDATELIB -p $BASEDIR/$BETTERPICKDIR $TARGET

# Drag sort list view
$UPDATELIB -p $BASEDIR/$DRAGSORTLIST $TARGET

#wizard pager
$UPDATELIB -p $BASEDIR/$WIZARD $TARGET

#Utils library
$UPDATELIB -p $BASEDIR/$UTILS $TARGET

# Main project
$UPDATEPROJ -p $BASEDIR/$NOTES $TARGET -l ../$UTILS -l ../$DRAGSORTLIST
