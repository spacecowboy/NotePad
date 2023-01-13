# How to contribute

Thank you for investing your time in contributing to this project.
Here you will get an overview of the contribution workflow.

## Contacting the developers

Developers are busy: their attention is a privilege, not a right.
We request that you value our time and don't waste it on inane matters.
That said, we are open to help anyone who wants to contribute, 
or simply to point out a bug. See below.

## Issues

Every discussion on the project happens on [the issues page](https://github.com/spacecowboy/NotePad/issues).
We don't use other platforms to discuss this app's development. 
Let's centralize everything there and make it easier for the maintainers to keep up. 
When creating a new issue, please use the templates and provide enough information.

## Translations

Use [weblate](https://hosted.weblate.org/engage/no-nonsense-notes/) to help us translate the app

## Testing

We use AndroidX and Espresso for android tests.
You can find the tests [here](../app/src/androidTest/java).
If you want to add tests, please separate them: those that use espresso should go
in their specific directory, separated from the others.
There is no special requirement for tests, just try to follow the code style you see in other test files.

## Submitting changes

Please send a GitHub Pull Request with a simple list of what you've done. 
Remember to follow our coding conventions (see below). 
Also, make sure all of your commits are atomic: only one feature per commit.
This project is not under very active development, so you may have to wait for a while before we answer.

## Coding conventions

We basically use the default Android Studio settings.
Before contributing, please run the Reformat Code tool (CTRL+ALT+L) on the `java` and `res` folders.
Write your code in english.
Avoid introducing new library dependencies (but AndroidX modules are fine).
In particular, avoid non-free libraries like Google Services or Dropbox.
Write your code in Java 11, not Kotlin: we value coherence across the codebase.

## What you can do

We have a list of tasks [here](https://github.com/spacecowboy/NotePad/issues/387).
Many of these are simple, so you can just pick one and work on it. Start from the top of the first post.
Or you can choose one of the open issues, preferably without the _secondary_ tag, but those may be harder to solve.
You **must tell us** that you started working on it. We could even help you get started!
