#!/bin/bash -eu

LATEST_TAG="$(git describe --tags "$(git rev-list --tags --max-count=1)")"
CURRENT_VERSION="$(git describe --tags)"

if [ -n "${SERVICEACCOUNTJSON:-}" ]; then
  cat > serviceaccount.b64 <<EOF
${SERVICEACCOUNTJSON}
EOF
fi

base64 --ignore-garbage --decode serviceaccount.b64 > serviceaccount.json

sed -i "s|/path/to/serviceaccount.json|$(pwd)/serviceaccount.json|" fastlane/Appfile

if [ -n "${KEYSTORE:-}" ]; then
  cat > keystore.b64 <<EOF
${KEYSTORE}
EOF

  base64 --ignore-garbage --decode keystore.b64 > keystore

  cat >> gradle.properties <<EOF
STORE_FILE=$(pwd)/keystore
STORE_PASSWORD=${KEYSTOREPASSWORD}
KEY_ALIAS=${KEYALIAS}
KEY_PASSWORD=${KEYPASSWORD}
EOF

fi

# Delete unsupported google play store languages
rm -rf fastlane/metadata/android/bs-BA \
   fastlane/metadata/android/eo \
   fastlane/metadata/android/tok \
   app/src/main/res/values-tok \
   fastlane/metadata/android/gl \
   app/src/main/res/values-gl \
   fastlane/metadata/android/eu \
   app/src/main/res/values-eu \
   fastlane/metadata/android/vec \
   app/src/main/res/values-vec \
   fastlane/metadata/android/he \
   app/src/main/res/values-he \
   fastlane/metadata/android/is \
   app/src/main/res/values-is

# to check if gradle actually receives the properties
cat gradle.properties

if [[ "${1:-}" == "--dry-run" ]] && [[ "${LATEST_TAG}" == "${CURRENT_VERSION}" ]]; then
  echo "${CURRENT_VERSION} is a tag but --dry-run was specified - not doing anything"
elif [[ "${1:-}" == "--dry-run" ]] || [[ "${LATEST_TAG}" != "${CURRENT_VERSION}" ]]; then
  echo "${CURRENT_VERSION} is not tag - validating deployment"
  if [[ "${CURRENT_VERSION}" =~ ^[0-9.]*$ ]]; then
    echo "${CURRENT_VERSION} is a production release"
    fastlane build_and_validate track:production
  else
    echo "${CURRENT_VERSION} is a beta release"
    # We don't have a beta track
    # fastlane build_and_validate track:beta
    fastlane build_and_validate track:production
  fi
else
  echo "${CURRENT_VERSION} is a tag - deploying to store!"
  if [[ "${CURRENT_VERSION}" =~ ^[0-9.]*$ ]]; then
    echo "${CURRENT_VERSION} is a production release"
    fastlane build_and_deploy track:production
  else
    echo "${CURRENT_VERSION} is a beta release"
    # We don't have a beta track
    # fastlane build_and_deploy track:beta
    fastlane build_and_validate track:production
  fi
fi

git checkout app fastlane gradle.properties
