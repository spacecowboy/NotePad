# An open source task manager
The might be confusing but that's historical as it started out as a simple note app. Syncing with Google Tasks
was just practical and along that path it went.

If you are developing an app of your own and have any questions on how the implementation details here, don't hesitate to drop me an e-mail or something!

# Getting sync to work
The app should work fine but sync will display an error message. To get it working you will need to put an API key in this file here:
https://github.com/spacecowboy/NotePad/blob/master/src/com/nononsenseapps/build/Config.java

You need to replace this line:

```java
public final static String GTASKS_API_KEY = "Put your key here";
```

But first you will of course need to get yourself a key. Follow the instructions on this page:
https://developers.google.com/google-apps/tasks/firstapp

Scroll to the section named __Register your project__.

Once you have put your key in the variable mention above, sync should work fine.
