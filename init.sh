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
NINEOLD="NineOldAndroids/library"
SHOWCASE="ShowcaseView/library"
UTILS="Utils"
NOTES="NoNonsenseNotes"

# The following is to make it all buildable with ant
#Makes sure everyone has the same support library
rm $BASEDIR/$NINEOLD/libs/android-support-v4.jar
rm $BASEDIR/$SHOWCASE/libs/android-support-v4.jar
rm $BASEDIR/$BETTERPICKDIR/libs/android-support-v4.jar

cp $BASEDIR/$NOTES/libs/android-support-v4.jar $BASEDIR/$NINEOLD/libs/
cp $BASEDIR/$NOTES/libs/android-support-v4.jar $BASEDIR/$SHOWCASE/libs
cp $BASEDIR/$NOTES/libs/android-support-v4.jar $BASEDIR/$BETTERPICKDIR/libs/

# Viewpager
$UPDATELIB -p $BASEDIR/$VIEWPAGERDIR $TARGET
#cd Android-ViewPagerIndicator/library;ant debug

# Better pickers
$UPDATELIB -p $BASEDIR/$BETTERPICKDIR $TARGET

# Drag sort list view
$UPDATELIB -p $BASEDIR/$DRAGSORTLIST $TARGET

#wizard pager
$UPDATELIB -p $BASEDIR/$WIZARD $TARGET

#ShowCaseView
$UPDATELIB -p $BASEDIR/$NINEOLD $TARGET
$UPDATELIB -p $BASEDIR/$SHOWCASE $TARGET

#Utils library
$UPDATELIB -p $BASEDIR/$UTILS $TARGET

# Main project
$UPDATEPROJ -p $BASEDIR/$NOTES $TARGET -l ../$UTILS -l ../$DRAGSORTLIST
