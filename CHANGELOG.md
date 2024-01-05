# 2.0.0
## main
- changed package name to `org.quiltmc.enigma` instead of `cuchaz.enigma`
  - you'd be amazed at how much of a struggle this was. I love gradle
- set up a separation between API and implementation classes, by moving them to the `org.quiltmc.enigma.api` and `org.quiltmc.enigma.impl` packages respectively
  - you can now trust that no breaking changes will be made to API until update `3.0`!
- added indexing for mappings
  - this goes in tandem with indexing for jars. index everything!
  - currently only has one indexer: an index of package names, that's used to warn the user that they're creating a new package in the UI
- more class references are now indexed in `EntryIndex` (pulled from Fabric upstream with permission!)
- we now allow using JSON lists for arguments to enigma services in your profile
  - to pull from iota's example when she implemented this, it means we don't have to do a `"custom_codecs": "nonsense, nonsense, nonsense, nonsense, you get the point"` disaster in our quilt profile
- redid name proposal API entirely!
  - the API is now neatly event-based, with a method that runs on jar load and a method that runs on mapping changes for you to hook into
  - established a clean separation between mappings proposed based on the bytecode (`JAR_PROPOSED`) and mappings proposed based on other mappings (`DYNAMIC_PROPOSED`)
  - we now track the *source plugin ID* of each proposed name, for debug reasons
  - added explicit priority for name proposal services: in the case of two services proposing for the same entry on the same event, the one higher up in your profile will take priority
- added a `getId()` method to `EnigmaService`
  - plugin IDs must now be explicitly defined in code, which cleans up name proposal quite a bit
- added lots more unit tests
- wrote up piles of documentation
- removed support for the old Recaf and Tiny V1 mapping formats
- removed old internal config API
## command
- implemented automatic format detection in all commands
- greatly improved command help
  - include descriptions of what each argument does in errors
  - add a dedicated `help` command
    - lists all possible commands
    - includes detailed descriptions of each command's function
- normalised all command names to `kebab-case`
## ui
- statistics got a huge rework
  - stat icons in the class tree dockers have been expanded to now show on packages as well as classes
  - optimise stat regeneration for single classes
  - stats are now stored persistently across the user interfaces
    - the stats dialog and stats tree now open instantly instead of making you wait for generation
    - stats are now generated for the full project at startup, meaning it'll be longer until the icons populate
    - navigating the class tree will be much snappier now that you're not starting generation tasks on every opened package
- added a "move package" option to the right-click context menu on packages, allowing you to move a package without refactoring surrounding packages
- added the new entry navigator, which allows you to quickly slide between different types of entry
  - this allows you to quickly navigate through long classes to find obfuscated entries
  - it can also be used to navigate between deobfuscated entries, entries with dynamically proposed names, and entries with jar-based proposed names
  - the entry navigator is pinned to the top-right corner of the code view window
- moved away from the [syntaxpane](https://codeberg.org/sciss/SyntaxPane) library in favour of [syntaxpain](https://github.com/QuiltMC/syntaxpain), a custom rai-engineered fork that is smaller, more maintainable, and hopefully faster
  - this fixes a couple long-standing UI bugs and should allow us to fix bugs faster as they crop up
  - it also enables more customisability and simplifies setup
- made some improvements to search dialog (pulled from Fabric upstream, thanks YanisBft for permission!)
  - pin exact matches to the top of the results
  - add a checkbox to only show exact matches
  - they changed the keystroke because `ctrl + space` doesn't work in all swing contexts. I already did that. thank you yanis for fuelling my ego
- the stats dialog now shows an overall percentage as well as parameters, fields, classes and methods individually
- moved over to quilt config for configuration reasons
  - cleaned up and organised config files
  - config is now in five files: `net`, `decompiler`, `keybind`, `main`, and `docker`
  - added a bunch of comments in the config to allow more editing directly in the file
  - your config will not be migrated! it won't be deleted or overridden, but if you've got some neat keybinds or something else you want to keep, make sure to reconfigure from the UI or copy things around the files
- added some neat debug utils if you run with the `--development` flag
  - currently, this includes a view of the internal mapping tree
- removed access modifier editing
## fixes
- fix yet more bugs with statistic accuracy
- fix the cancel button in the "rename package" window causing a crash
- fix documenting constructors (from Fabric upstream, with permission!)
- fix an untranslated string that could appear in logs (`tiny_v2.loading` for the curious)
- fix a rare NPE on startup (that weird one with the icons. I saw it like 1/20 times. if you're like me you're very happy about this. if you're not that's ok I love everyone)
- fix some issues with incorrect server docs

# 2.1.0

## new features 
- added new development options!
	- added an option to enable debug token highlighting
	- added an option to show the source plugin of proposed mappings in the identifier panel
	- remember to have these options available you need to run enigma with the `--development` flag!
- added tokenization to the bytecode view
	- this allows you to rename entries and more clearly see what is obfuscated and deobfuscated when viewing the bytecode!
- added syncing of names between method declarations in interfaces
	- sometimes, a class will implement methods with the same names and signatures from two different interfaces. previously, this would break the mappings if you renamed one of those as enigma wasn't aware that they needed to match. now it's smarter!

## bugfixes. a lot of them
- fixed the server asking all users to verify warnings instead of just the mapper who caused the warning
- fixed mappings sync on the server sending proposed mappings
- fixed some issues with `drop-invalid-mappings`
- fixed an inverted condition when parsing arguments in the CLI
- fixed toggling between obfuscated and deobfuscated on parameters of bridge methods
- fixed vineflower settings not being properly applied and saved
- fixed the merged mapping tree occasionally prioritising empty mappings over real ones
- fixed incorrect bytecode tokens on arrays
- fixed recent projects adding duplicate entries when the same project is opened twice
- fixed old recent projects beyond the limit not being removed
- fixed a startup crash when no recent projects are present
- fixed a startup crash likely caused by a bug in FlatLAF
- fixed the package index being quite broken and not properly tracking all packages
