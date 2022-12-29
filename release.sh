#!/bin/bash

# Run checks and prepare a release build
#
# Credits to Feeder by @spacecowboy
# https://gitlab.com/spacecowboy/Feeder/

set -u

TARGET="${1:-HEAD}"


current_default="$(git describe --tags --abbrev=0 "${TARGET}")"


echo >&2 -n "Current version  is ${current_default}"
# read -r current_in

next_default="$(grep "versionName" ./app/build.gradle | sed "s|\s*versionName \"\(.*\)\"|\\1|")"
echo >&2 -n "Next version [press 'enter' for ${next_default}]: "
read -r next_in

if [ -z "${next_in}" ]; then
  NEXT_VERSION="${next_default}"
else
  NEXT_VERSION="${next_in}"
fi

CURRENT_CODE="$(grep "versionCode" ./app/build.gradle | sed "s|\s*versionCode \([0-9]\+\)|\\1|")"
echo >&2 "Current code ${CURRENT_CODE}"

# The old version was 57130, so we have to use the thousands for the minor code, like 70100.
# The last 2 digits are useless, but it can't be helped
next_code_default=$(( CURRENT_CODE + 100 ))

echo >&2 -n "Next code [press 'enter' to confirm ${next_code_default}]: "
read -r next_code_in

if [ -z "${next_code_in}" ]; then
  NEXT_CODE="${next_code_default}"
else
  NEXT_CODE="${next_code_in}"
fi

read -r -p "Check consistency of languages list with values- folders? [y/N] " response
if [[ "$response" =~ ^[yY]$ ]]
then
  ./gradlew :app:checkLanguages
fi

# changelog template. To add all the new commit messages, move this line inside the ""
# $(git shortlog -w76,2,9 --format='* [%h] %s' ${current_in}..HEAD)
CL="NoNonsense Notes v${NEXT_VERSION}

Highlights:
- FILL AND SAVE THIS FILE

Details:
-
"

tmpfile="$(mktemp)"

echo "${CL}" > "${tmpfile}"

if hash notepad 2>/dev/null; then
  # edit with notepad
  notepad "${tmpfile}"
else
  # fallback
  nano "${tmpfile}"
fi

echo >&2 "Changelog for [${NEXT_VERSION}]:"
cat >&2 "${tmpfile}"

read -r -p "Write changelog? [y/N] " response
if [[ "$response" =~ ^[yY]$ ]]
then
  # Playstore has a limit
  head --bytes=500 "${tmpfile}" >"fastlane/metadata/android/en-US/changelogs/${NEXT_CODE}.txt"
fi

# update versions on build.gradle
read -r -p "Update gradle versions? [y/N] " response
if [[ "$response" =~ ^[yY]$ ]]
then
  sed -i "s|\(\s*versionCode \)[0-9]\+|\\1${NEXT_CODE}|" app/build.gradle
  sed -i "s|\(\s*versionName \).*|\\1\"${NEXT_VERSION}\"|" app/build.gradle
fi

read -r -p "Commit changes? [y/N] " response
if [[ "$response" =~ ^[yY]$ ]]
then
  git add "fastlane/metadata/android/en-US/changelogs/${NEXT_CODE}.txt"
  git add app/build.gradle
  git commit -m "Releasing ${NEXT_VERSION} from release.sh"
fi

read -r -p "Make tag? [y/N] " response
if [[ "$response" =~ ^[yY]$ ]]
then
  git tag -am "$(cat "${tmpfile}")" "${NEXT_VERSION}"
fi

echo "Status of git files:"
git status -s

read -r -p "Push files and tags to remote? [y/N] " response
if [[ "$response" =~ ^[yY]$ ]]
then
  git push
  git push --tags
fi

# github page to create releases
WEBPAGE=https://github.com/spacecowboy/NotePad/releases/new
echo $WEBPAGE

read -r -p "Open that github page to make a new release? [y/N] " response
if [[ "$response" =~ ^[yY]$ ]]
then
  if hash start 2>/dev/null; then
    # open URL on windows
    start $WEBPAGE
  else
    # open URL on linux
    xdg-open $WEBPAGE
  fi
fi
