# NoNonsense Notes

[![Android build](https://github.com/spacecowboy/NotePad/actions/workflows/android_build.yml/badge.svg)](https://github.com/spacecowboy/NotePad/actions/workflows/android_build.yml)          [![Android tests](https://github.com/spacecowboy/NotePad/actions/workflows/android_tests.yml/badge.svg)](https://github.com/spacecowboy/NotePad/actions/workflows/android_tests.yml)     [![Translation status](https://hosted.weblate.org/widgets/no-nonsense-notes/-/android-strings/svg-badge.svg)](https://hosted.weblate.org/engage/no-nonsense-notes/) \
<img src="https://img.shields.io/f-droid/v/com.nononsenseapps.notepad.svg?logo=F-Droid"/> <img src="https://img.shields.io/github/release/spacecowboy/NotePad.svg?logo=github"/> <img src="https://img.shields.io/github/release-date/spacecowboy/NotePad"/> <img src="https://img.shields.io/github/downloads/spacecowboy/NotePad/latest/total"/> \
<img src="https://img.shields.io/github/last-commit/spacecowboy/NotePad"/> <img src="https://img.shields.io/github/search/spacecowboy/NotePad/TODO"/> <img src="https://img.shields.io/librariesio/github/spacecowboy/NotePad"/> \
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/repository/browse/?fdid=com.nononsenseapps.notepad) \
_Note-taking app for Android with reminders, since 2012._

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" alt="Phone UI" height="480" /><img src="fastlane/metadata/android/en-US/images/tenInchScreenshots/1.png" alt="Tablet UI" height="360" />

## Translation

Help translate the app on [Hosted Weblate](https://hosted.weblate.org/projects/no-nonsense-notes/) \
<a href="https://hosted.weblate.org/engage/no-nonsense-notes/">
<img src="https://hosted.weblate.org/widgets/no-nonsense-notes/-/horizontal-auto.svg" alt="Translation status" />
</a>

## Reporting bugs

Please [report bugs](https://github.com/spacecowboy/NotePad/issues) explained in clear steps using the provided template.

## Build the project

```sh
git clone https://github.com/spacecowboy/NotePad
cd NotePad
./gradlew check
./gradlew installDebug
```

if it does not work, [open an issue](https://github.com/spacecowboy/NotePad/issues)

## Where did Google Tasks sync go?

[Discussed here](https://github.com/spacecowboy/NotePad/issues/426)

## Where are the files saved?

The app can save 2 kinds of files:
* ORG files, used for SD card synchronization. They are saved in a subfolder of `Android/data/` where they will be visible to your file manager, if you want to manually edit them
* JSON files, for the backup-restore functionality. These are saved wherever you want

## GPLv3+ License

<details>

```text
Copyright (C) 2014 Jonas Kalderstam

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
```

</details>

[Full license](LICENSE)

## Useful links

* [FAQ](app/FAQ.md)
* [Contribution guide](CONTRIBUTING.md)
* [Releases](https://github.com/spacecowboy/NotePad/releases)
* [Privacy policy](PRIVACY_POLICY.txt)

Built by @spacecowboy, maintained by @CampelloManuel. \
The app is currently being updated. Old versions are still available on F-droid.
