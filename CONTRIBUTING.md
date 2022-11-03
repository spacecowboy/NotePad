# How to contribute

Thank you for investing your time in contributing to our project.

In this guide you will get an overview of the contribution workflow from opening an issue, creating a PR, and merging the PR.

Use the table of contents icon on the top left corner of this document to get to a specific section of this guide quickly.

## Issues

When creating a new issue, please use the templates and provide enough information.

## Testing

We use AndroidX and Espresso for android tests.

You can find the tests [here](/app/src/androidTest/java/com/nononsenseapps/notepad).

If you want to add tests, please separate them: those that use espresso should go in their specific directory, separated from the others.

There is no special requirement for tests, just try to follow the code style you see in other test files.

## Submitting changes

Please send a GitHub Pull Request with a simple list of what you've done. 
Remember to follow our coding conventions (see below). 
Also, make sure all of your commits are atomic: only one feature per commit.
This project is not under active development, so you may have to wait for a while before we answer.

## Coding conventions

We basically use the default Android Studio settings, with some tweaks.
Before contributing, please run the Reformat Code tool (CTRL+ALT+L) on the `java` and `res` folders separately.

Everything should be written in english, except of course the translated string resources.
