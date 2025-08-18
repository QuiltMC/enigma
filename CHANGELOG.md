# 2.0.0

The largest Enigma update ever brings you dozens of changes across the user interface, the API, the command line!

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
- implemented automatic format detection in all commands
- greatly improved command help
  - include descriptions of what each argument does in errors
  - add a dedicated `help` command
    - lists all possible commands
    - includes detailed descriptions of each command's function
- normalised all command names to `kebab-case`
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
- fix yet more bugs with statistic accuracy
- fix the cancel button in the "rename package" window causing a crash
- fix documenting constructors (from Fabric upstream, with permission!)
- fix an untranslated string that could appear in logs (`tiny_v2.loading` for the curious)
- fix a rare NPE on startup (that weird one with the icons. I saw it like 1/20 times. if you're like me you're very happy about this. if you're not that's ok I love everyone)
- fix some issues with incorrect server docs

# 2.1.0

As with any big release, Enigma 2.0 broke tons of things. We've cleaned up all the bugs and crashes as well as added a few features for the bytecode nerds with this release!

- added new development options!
	- added an option to enable debug token highlighting
	- added an option to show the source plugin of proposed mappings in the identifier panel
	- remember to have these options available you need to run enigma with the `--development` flag!
- added tokenization to the bytecode view
	- this allows you to rename entries and more clearly see what is obfuscated and deobfuscated when viewing the bytecode!
- added syncing of names between method declarations in interfaces
	- sometimes, a class will implement methods with the same names and signatures from two different interfaces. previously, this would break the mappings if you renamed one of those as enigma wasn't aware that they needed to match. now it's smarter!
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

# 2.2.0

2.2.0 brings tons of updates to the Enigma server, making it actually usable, improves the identifier panel, and adds a new `print-stats` command!

- added more info to the identifier panel:
  - for inner classes, the name of their parent class is now displayed
  - for parameters, their type is now shown. this is super helpful for mapping lambdas where you can't see their type!
- made some big changes and cleanups to the enigma server!
  - the protocol version is now `2`!
  - made lots of architectural changes to streamline the code
  - greatly improved the way client approval is handled
  - added a development option to log packets
  - added a username field to the server creation dialog
  - added validation for the client's username
  - fixed lots of possible crashes (if you wanted to, it would be trivially easy to crash an enigma <`2.2.0` server with a custom client)
  - added lots of new tests to make sure everything is working!
- added a new command: `print-stats`
  - this command generates and prints mapping stats for on-the-go mappers who can't be bothered to crack open the swing GUI
- added dynamic resizing for the calls tree docker's list of tokens
- added a new shortcut constructor for `LocalVariableEntry`
- added tokenization of invokedynamic arguments (this means something if you're a nerd. we promise)
- fixed some users not seeing indentation when using the vineflower decompiler
- updated [syntaxpain](<https://github.com/QuiltMC/syntaxpain>) to `1.1.2`
  - fixes the line ruler in the code view not automatically resizing
- updated [quilt config](<https://github.com/QuiltMC/quilt-config>) to `1.2`
- updated [vineflower](<https://github.com/Vineflower/vineflower>) to the latest `1.10.0` snapshot

# 2.2.1

The last release had a few bugs. They're gone now!

- make sure MappingValidator eliminates equivalent entries from consideration -- solves some false positives
- remove an unnecessary null check for inner classes' outer classes (the java standard allows inner classes to have no outer class)
- fixed crashes that could be caused by deselecting "decompile inner classes" in vineflower
- fixed incorrect synchronisation of javadoc on servers
- fixed a possible crash when writing mappings that include a packageless class

# 2.3.0

As always, we've hard at work improving the beautiful software that helps mappers map. Today, we get to release all the fixes we've been working on for the last couple months!

- added inactive states for inheritance tree dockers
    - this includes the implementations docker and inheritance docker. when they have no inheritance to display, they will show messages informing the user of their state
- added a progress bar in the bottom right corner to indicate stat generation progress
    - this progress bar is usable for other work as well, and will be used to display other things in the future!
- added prettier printing for field types in the identifier panel
    - matches the printing used by lambda types
- whitespace is now automatically stripped when creating `EntryMapping` objects
- development options menu is now always shown if any development options are enabled
- added a config option for disabling stat icons
- fixed stat icons not matching the currently editable types
- fixed stat generation having the wrong amount of total work
- fixed entry navigator not updating after name proposal
- fixed checkboxes not displaying properly in the search panel
- added a processor to display all possible language choices in the config file
- updated dependencies
    - ASM: `9.6` -> `9.8-20240409.145119-3`
        - this version is a snapshot. if you want to use enigma `2.3`, you'll need to include the ow2 snapshot repository in your buildscript via adding `maven { url = "https://repository.ow2.org/nexus/content/repositories/snapshots/" }` to your `repositories` block
    - vineflower: `1.10.0-20240126.053810-85` -> `1.10.0`
        - you can now *remove* the vineflower snapshot repository from your buildscripts!
    -  quilt config: `1.2.0` -> `1.3.0`
    -  syntaxpain: `0.1.2` -> `0.1.5`
    -  cfr: `0.2.1` -> `0.2.2`
    -  guava: `32.0.1` -> `33.0.0`
    -  flatlaf: `3.2.5` -> `3.4`

# 2.3.1

2.3.1 is your quickest bugfix release yet.

- fixed fractional scaling not applying to docker buttons
- fixed ASM artifact expiring

# 2.4.0

Enigma `2.4` represents some huge internal changes that will have some big consequences for your plugins! These consequences may not be pretty, but they're for your own good. Trust us. The future is bright for Enigma plugins.

With this release, we made quite a few changes in the direction of supporting more mapping formats through third-party libraries, in self-contained plugins to make sure not to complicate Enigma's internal spaghetti more.

- made some major API changes!
  - added a new service type: read/write services
    - this service defines an optional reader and an optional writer for mappings
    - each service is keyed by a unique file type, of which the uniqueness is determined by the extension
    - this is a major breaking change to reading and writing, and will impact your plugins! make sure to test your plugins when updating to `2.4`.
    - this change adds more modularity to enigma, which will be useful as we look to support libraries like Fabric's [mapping-io](https://github.com/FabricMc/Mapping-IO)
  - added validation for plugin registration
    - no two services of a given type can have the same ID
    - plugins' IDs must strictly adhere to the rules defined in `EnigmaService` javadoc
    - no two read/write services can support the exact same file type
  - service types now support being active by default
    - this type of service does not have to be explicitly defined in a profile to be active
    - being defined explicitly in the profile will override this behaviour
    - decompiler services and read/write services are active by default
- added filters to the "open mappings" dialogue, making it easier to search for mappings
  - this is based on a [PR to Fabric](https://github.com/FabricMC/Enigma/pull/532) by [Juuz](https://github.com/Juuxel), with some improvements
  - supports filtering for multiple file types at once
  - by default open mappings will still auto-detect the format, filtering for all supported file types
- fixed issues with resolving matching entries
  - this fixes some false positives leading to invalid mappings being written
- added validation for lambda parameter name uniqueness
  - this is yet another recompilability issue fixed!
  - previously, you could rename a lambda parameter to a name that conflicted with other local variables, causing crashes when compiling the named code
  - this applies recursively to all parameters, meaning that parameters of a top-level method can conflict with its internal lambdas. this ensures that all possible conflicts are eliminated
- fixed a lot of scale issues!
  - fixed highlight boxes not scaling
  - fixed text outside of the code view not scaling on themes other than default
  - fixed various small issues
- fixed the identifier panel reporting the wrong type for some lambda and method parameters
- added lots of new unit tests and documentation as always!
- updated gradle and fixed deprecations
- updated dependencies
  - vineflower: `1.10.0` -> `1.10.1`

# 2.4.1

As always, we need a quick bugfix release before the next big update. `2.4.1` fixes some long-standing issues!

- fixed javadocs applied to parameters with proposed names becoming detached from the parameter due to being saved incorrect in the Enigma mappings format
- fixed the GUI still attempting to confirm warnings when a fatal error is present in a validation
- updated dependencies
  - guava: `33.0.0` -> `33.2.1`
  - proguard: `7.4.0` -> `7.5.0`

# 2.4.2

More bugfixes. I work so hard for my beloved users.

- fixed javadoc dialog not scaling properly
- fixed invalid data being appended to exports of the stats tree
- removed an extra debug print statement that was sitting in the mass package renamer
- updated dependencies
  - quilt config: `1.3.0` -> `1.3.2`
  - junit: `5.9.3` -> `5.10.3`
  - hamcrest: `2.2` -> `3.0`
  - jimfs: `1.2` -> `1.3.0`

# 2.5.0

`2.5.0` brings contributions from 3 of our lovely enigma users in addition to our regular duo, bringing you some reworks to long-broken features and fixing some long-bothersome bugs!

- new [theme system and theme](https://github.com/QuiltMC/enigma/pull/216) (thanks [supersaiyansubtlety](https://github.com/supersaiyansubtlety)!)
  - adds a new theme: darcerula
    - this theme is yet darker than the old dark theme, for those that appreciate a pitch black atmosphere for their mappings
  - moves theme configurations into a separate `/theme/` directory in the config folder
    - declutters the `main.toml` file
    - old theme configs will not be migrated, you'll have to manually transfer your old custom themes
  - cleans up a lot of backend for themes, allowing us to easily add new themes in the future
- added indexing for libraries in addition to the main JAR
  - separate step from normal indexing, performed after
  - disabled by default for all existing plugin-based indexers
    - can be enabled by setting the `index_libraries` property to true in service's config in the enigma profile
    - refer to Javadocs in `JarIndexerService` for how to implement this property, we recommend adding it!
  - currently, only `Record` and `Object` from the JDK are indexed as libraries by default
- API changes around indexing
  - `JarIndex#indexJar` no longer receives a scope
    - the `ClassProvider` parameter has been replaced with a `ProjectClassProvider`, providing classes and scope for both the main jar and libraries
  - `JarIndexerService#acceptJar` now takes a `ProjectClassProvider` instead of a `ClassProvider`
    - the scope has not been removed, and if the service is configured to accept libraries will be the main scope on first run and the library scope on the second
- added name proposal for record components
  -  names for record getters are automatically proposed as their corresponding field is named
  -  methods are linked to fields based on bytecode
    - this is a fail-fast solution: if there is no method perfectly matching the expected code for a record getter no mapping will be proposed
    - this allows us to sucessfully propose mappings in situations such as [hashed mojmap](https://github.com/quiltmc/mappings-hasher) where the record getter method mismatches with the component name
  - works using two new services: `enigma:record_component_indexer` and `enigma:record_component_proposer`
- deprecated `EntryMapping#DEFAULT` to be renamed to `EntryMapping#OBFUSCATED`
- fixed proposed method validation and main plugin id validation using different regexes to validate
  - it was possible to write a valid plugin ID that would crash when used on a proposed mapping
- fixed issues with stat generation and records
  - ignore parameters of canonical constructors for records as they can be hidden by decompilers
  - ignore parameters of equals() method for the same reason
- fixed mapping stats filtering
  - fixed issues with GUI when using dots to filter
  - fixed issues with graph when using slashes to filter
- fixed a crash when cancelling a class rename initiated from the class tree (thanks [notevenjoking](https://github.com/770grappenmaker)!)
- fixed a possible crash when parsing recent files (thanks [pitheguy](https://github.com/PiTheGuy)!)
- fixed entry navigator ignoring which types are currently editable
- fixed mappings chooser not accepting directories
- fixed missing translations in mappings chooser
- fixed a bunch more scaling issues (thanks [supersaiyansubtlety](https://github.com/supersaiyansubtlety) again!)
  - fixed config values sometimes being messed up when changing scale and restarting
  - fixed editor font size sometimes being overwritten
- fixed possibly incorrect save location when saving from the unsaved warning dialogue (thanks again [pitheguy](https://github.com/PiTheGuy)!)

# 2.5.1

Hot off the tail of `2.5`, enigma `2.5.1` features some minor improvements to `drop-invalid-mappings` and the usual wealth of bugfixes.
But honey, I know you're just here to see if you can remove the ASM snapshot repo from your buildscript. I'm happy to report that you can!

- improved `drop-invalid-mappings` command
  - improved logging
    - do not print lines about writing new mappings when no changes have occurred
    - print stats after completion on how many mappings were dropped
  - improved behaviour for dropping
    - drop methods that have no name and no valid parameters
    - drop parameters whose index is outside their parent method's scope of valid indices
  - added unit testing
- fixed various issues with javadoc on parameters
  - fixed comments on parameters sometimes being improperly written by the tinyv2 writer
  - fixed javadoc on method overrides not properly finding parameter names
  - fixed javadoc not always refreshing on parameter name updates
- fixed entry navigator pointing to the wrong entry after an entry's token type was changed
  - the most common time this would occur was when you renamed an obfuscated entry with the default navigator, and since there were then a different amount of obfuscated entries, the navigator would point to a different one than previously
  - this fix is thanks to [pitheguy](https://github.com/PiTheGuy)!
- fixed identifier panel mislabelling inner classes' outer class as their "superclass"
- fixed stats of parent classes not reloading when their entries are mapped from a child class
- fixed folder icons in the "obfuscated classes" docker not being visible
- updated dependencies
  - asm: `9.8-SNAPSHOT` -> `9.7.1`
    - you can now remove the ASM snapshot repo from your buildscript when depending on enigma through maven/gradle!

# 2.5.2

In our bugfix era.

- fixed stability issues introduced with the new validation on parameter indices
- added missing translations for library indexing
- fixed entry navigator pointing to negative entry indices after renaming items (thanks [pitheguy](https://github.com/PiTheGuy)!)

# 2.5.3

in the code straight up 'fixin it'. and by 'it' let's justr say. my bugs

- fixed two issues with `dropInvalidMappings`
  - fixed recursive search for mapping sometimes failing and dropping parents of mapped entries
  - fixed an extremely rare case where methods with improper `max_locals` attributes would lose their args

# 2.6.0

My beloved users. Did you miss me?
The latest release of everyone's favourite program features major upgrades to both name proposal in the backend and package renaming in the GUI.

- improved package renaming in the GUI
  - fixed various possible crashes
  - a confirmation dialogue containing samples of renames to be applied will now be shown before applying renames
    - this ensures that an unexpected result will not be produced, as there is no undo functionality currently
  - made testing for package renaming logic more robust
- made some major changes to name proposal
  - added a new boolean in `NameProposalService`: `isFallback()`
    - this boolean marks all mappings proposed by the service as fallback, changing their behaviour
    - fallback names will not count towards stats by default
    - fallback names will have a different colour in the UI
    - fallback names do not have their own token types, and are expected to be less reliably high-quality names than normal proposed mappings
  - added a new method in `NameProposalService`: `validateProposedMapping(Entry<?>, EntryMapping, boolean)`
    - this method is called on proposed mappings to validate them
    - this can be used to reduce or heighten the strictness of validating mappings proposed by your service
    - do not override this method unless you know what you're doing!
  - dynamically proposed names will now also be validated
    - this may break plugins using previously unsupported behaviour, such as proposing token types other than `DYNAMIC_PROPOSED`
  - added an `Enigma` object as context for bytecode-based name proposal
    - an `Enigma` object is also now accessible in dynamic proposal via the `EntryRemapper` object
  - greatly increased unit testing for name proposal
- added new `readMappings(Path)` and `readMappings(Path, ProgressListener)` API method to `Enigma`
  - these are a simple way to read mappings!
- added extensive class-level javadoc for `TypeDescriptor`
- fixed `dropInvalidMappings` taking two runs to successfully drop all invalid/empty mappings in some cases

# 2.6.1

day two bugfix release for my all name proposal girlies out there in the world

- fix an NPE that could occur when proposing a null mapping

# 2.6.2

A wealth of bugfixes brought to you by rai, iota, and friends. You'll be rich. Take my hand.

- fixed CFR and vineflower not allowing renames on methods named 'new'
- fixed renames being started when pressing non-letter keys such as escape while hovering an entry (thanks [pitheguy](https://github.com/PiTheGuy)!)
- fixed inheritance for JRE classes not being indexed
- fixed a possible stack overflow in `IndexEntryResolver`
- fixed an issue with similarly-named classes causing the token highlighter to explode (thanks [pitheguy](https://github.com/PiTheGuy)!)
- fixed translations for mapping formats not properly displaying
- fixed deobfuscation level icons not immediately updating after stat generation completes (thanks [pitheguy](https://github.com/PiTheGuy)!)
- improved wording of some save dialogs (thanks [pitheguy](https://github.com/PiTheGuy)!)
- improved `PackageIndex` javadoc
