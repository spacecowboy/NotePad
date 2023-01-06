# FAQ

**Q: Why is a task manager named "Notes"?**

A: The app started with only the note taking features. Syncing with Google Tasks seemed like a good
idea. Then it only made sense to support the rest of the features of Google Tasks. Sadly, due to
limitations from Google, this feature is not available anymore.

**Q: Can you make a theme with yellow background and black lines?**

A: Absolutely not. I designed the app to get away from things like that. If you want skeuomorphic
design, go buy an iPhone.

**Q: Will you add colors/priority to notes?**

A: Short answer: no. Long answer: I try hard to keep the app simple. Adding more features like this
would make it more complicated. There are plenty of task managers out there that have more features
than my app. I don't use those features and hence I don't want them in my app. Until I see a real
use for them, they won't be implemented.

**Q: I want to have a check list inside a note. Can you add this? Task program X has it**

A: No. That would go against the entire structure of the app. If you want a shopping list for
example, create a list called "Shopping list". Then you have a checklist for your shopping. Don't be
afraid to create lists, and then delete them when you're done with them.

**Q: Will I ever be able to set sub tasks?**

A: No. At this point you should look into a more complicated app, like orgzly.

**Q: Can you add a save button?**

A: It's a useless button. Notes are saved when they are closed or when the screen goes dark. Learn
to trust it.

**Q: what files can I save?**

The app can save 2 kinds of files:

* ORG files, used for SD card synchronization. 
  * They are saved in a subfolder of `/storage/emulated/0/Android/data/`
  * they will be visible to your file manager app, if you want to manually edit them
* JSON files, for the backup-restore functionality
  * you can save these wherever you want

If you are not satisfied, [open an issue](https://github.com/spacecowboy/NotePad/issues/new/choose)
and recommend a behavior.

**Q: Why are the files saved there?**

A: Newer Android versions limit what we can do when working with files. See https://github.com/spacecowboy/NotePad/issues/454

**Q: I opened a pull request or an issue before 2022. Why did you close it?**

A: The project was restarted in 2022, and many things changed. Plans for a minimal 
6.0.0 version were dropped, and the current 7.0.0 version inherits directly from 5.7.1, which
was last released in 2015. Many things changed in those 7 years, so we will take care of new
issues and pull requests as they appear. Apologies for the inconvenience.
