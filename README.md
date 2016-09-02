# NoNonsense Notes

<a href="https://flattr.com/submit/auto?user_id=spacecowboy&url=https%3A%2F%2Fgithub.com%2Fspacecowboy%2Fnotepad%2F" target="_blank"><img src="http://api.flattr.com/button/flattr-badge-large.png" alt="Flattr this" title="Flattr this" border="0"></a> [![Build Status](https://travis-ci.org/spacecowboy/NotePad.svg?branch=master)](https://travis-ci.org/spacecowboy/NotePad) [![Crowdin](https://d322cqt584bo4o.cloudfront.net/nononsensenotes/localized.png)](https://crowdin.com/project/nononsensenotes)

[![F-Droid](https://f-droid.org/wiki/images/0/0f/F-Droid-button_smaller.png)](https://f-droid.org/repository/browse/?fdid=com.nononsenseapps.notepad)

Link to [Google Play](https://play.google.com/store/apps/details?id=com.nononsenseapps.notepad)

Latest updates posted on [http://www.nononsenseapps.com](http://www.nononsenseapps.com)

<img src="tablet.png" alt="Tablet UI" />

<img src="phone.png" alt="Phone UI" height="720" />

# Reporting bugs
Please report bugs by creating an [issue here](https://github.com/spacecowboy/NotePad/issues).
I'd very much appreciate it if you could write clear steps to reproduce the bug. An example would be:

```
Title: Wrong due date set for completed notes

Steps to reproduce:
1 - Press + to create a new note
2 - Add some text
3 - Set a due date for yesterday (2014-11-24)
4 - Mark note as compeleted by ticking the checkbox
5 - Press <- to save the note

Result:
Note is displayed with a due date of today (2014-11-25) in the list.

Expected result:
Note should have the due date I set earlier.
```

If relevant, please say if you're using a phone or a tablet (UI-issues), or what your sync settings are (odd stuff happening in general).

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

Same as above but including Dropbox support:

    ./gradlew installPlayDebug

# License

```
Copyright (C) 2015 Jonas Kalderstam

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
