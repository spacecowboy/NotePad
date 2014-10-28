# NoNonsense Notes

[![Build Status](https://travis-ci.org/spacecowboy/NotePad.svg?branch=master)](https://travis-ci.org/spacecowboy/NotePad)

Link to [Google Play](https://play.google.com/store/apps/details?id=com.nononsenseapps.notepad)

Latest updates posted on [http://www.nononsenseapps.com](http://www.nononsenseapps.com)

<img src="tablet.png" alt="Tablet UI" />

<img src="phone.png" alt="Phone UI" height="720" />

# Getting sync to work
You need to put your API keys in a file, like the sample here:
https://github.com/spacecowboy/NotePad/blob/master/core/assets/secretkeys.properties.sample

But first you will of course need to get yourself a key. Follow the instructions on this page:
https://developers.google.com/google-apps/tasks/firstapp

Scroll to the section named __Register your project__.

And similar over here https://www.dropbox.com/developers/apps

# Build the project

For free version:

    ./gradlew installFreeDebug

Or with play services and location reminders:

    ./gradlew installPlayDebug

Same as above but including Dropbox at the moment:

    ./gradlew installPlayBetaDebug

# License
     Copyright (C) 2014 Jonas Kalderstam

     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
