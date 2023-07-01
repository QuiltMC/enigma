## changes since forking from cuchaz's original enigma

One large change since the fork has been splitting the project into subprojects: `enigma`, `enigma-cli`, `enigma-swing`, and `enigma-server`.
The changes will be split into changes to `enigma`, `enigma-cli`, and `enigma-swing`, since `enigma-server` is an addition post-fork.
We'll only be cataloguing major changes, since there are countless bug fixes to go through. The addition of `enigma-server` introduces multiplayer support.

### general

- added multiplayer support
- reworked gradle setup

### enigma

- implemented support for new mapping formats, such as enigma's directory format and fabric's tiny
- implemented support for remapping local variables
- ported to an ASM backend
- piles of minor optimisations
- index references of fields and methods
- added support for plugins, allowing features like name proposal
- redo the majority of backend code
- greatly improved rename validation
- added support for newer java features such as records
- added translation support, with other languages including french and japanese
- improved stats generation to fix inaccuracies and show percentages
- added piles of new tests and reworked testing
- migrated logging to tinylog

### enigma-cli

- implemented a command for converting mapping formats
- implemented a command to check mappings
- refactored to match the style of fabric's stitch
- implemented a command to drop invalid mappings
- implemented a command to fill class mappings
- added support for running commands directly

### enigma-swing
- added a pile of hotkeys, such as one for reloading mappings
- added configurability to keybinds
- some small improvements to the decompiled code view, such as allowing selection of text and coloring line numbers
- added support for custom themes, and added default themes such as dark theme
- made decompilation parallel for speed
- added a search function
- added support for decompilation with CFR
- added a configurable scale factor
- added tabs for editing multiple classes at once
- added a popup menu to class selectors
- added a selector for all classes
- added icons to class tree, including type and stats
- added a docker system for modules outside the class view
- added new dockers: structure docker, notifications docker, collab docker, all classes docker
- added support for flatlaf
- improved token highlighting with different colour signifying different things
- add more information to info panel, such as obfuscated name
- added nested packages in class selectors
- added support for decompilation with quiltflower
- added storage of recent projects
- reworked notification system
- added support for renaming entire packages
- improved progress bars with more information
- added bytecode view
- added crash history
- added tooltips to class selectors
