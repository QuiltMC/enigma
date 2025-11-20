# Contributing

Thank you for your interest in contributing to Enigma!

We recommend discussing your contribution with other members of the community - either directly in your pull request,
or in our other community spaces. We're always happy to help if you need us!

Enigma is distributed under the [LGPL-3.0](LICENSE).

## Translating
Translations are loaded from [enigma/src/main/resources/lang/](enigma/src/main/resources/lang/).

These are the currently supported languages and their corresponding files:

| Language                   | File         |
|----------------------------|--------------|
| English (U.S.) **default** | `en_us.json` |
| Chinese (Simplified)       | `zh_cn.json` |
| French                     | `fr_fr.json` |
| German                     | `de_de.json` |
| Japanese                   | `ja_jp.json` |

If a language you'd like to translate isn't on the list, feel free to ask for help on
[Quilt's Discord Server](https://discord.quiltmc.org/)!

### Search Aliases
Many elements in Enigma's GUI support search aliases, but most don't have any aliases.
A full list of search alias translation keys is [below](#complete-list-of-search-alias-translation-keys).
Search aliases are alternative names for an element that a user might search for when looking for that element.
For example, the `Dev` menu element has two search aliases: `"Development"` and `"Debugging"`. This means that if a user
searches for "Debug", the `Dev` menu will be a search result.

Search aliases are language-specific, so there's no need to translate the English aliases if they aren't likely
to be searched for in your target language. In fact, any language may add additional aliases that aren't present in the
English translation.

Since elements can have multiple search aliases, their translations can be lists. Aliases are separated by `;`.<br>
For example, the `Dev` menu's aliases look like this in the translation file: `"Development;Debugging"`
This means that aliases may not contain the `;` character.

Some things to keep in mind when adding search aliases:
- elements' names are always searchable; there's no need to add their names to their aliases
- searching is case-insensitive, so there's no need to add variations that only differ in capitalization
- searching matches prefixes, so there's no need to add variations that are prefixes of one another,
just add the longest variation (note that the element name may be a prefix of an alias, as is the case with `Dev`'s
`"Development"` alias)

If you'd like to add search aliases to an element that doesn't already have aliases, add its alias translation key to
its translation file.

#### Complete list of search alias translation keys
| Element                                      | Translation Key                                   |
|----------------------------------------------|---------------------------------------------------|
| `Dev` menu                                   | `"dev.menu.aliases"`                              |
| `Collab` menu                                | `"menu.collab.aliases"`                           |
| `Decompiler` menu                            | `"menu.decompiler.aliases"`                       |
| `Help` menu                                  | `"menu.help.aliases"`                             |
| `Search` menu                                | `"menu.search.aliases"`                           |
| `Crash History` menu                         | `"menu.file.crash_history.aliases"`               |
| `File` menu                                  | `"menu.file.aliases"`                             |
| `File`>`Open Jar...`                         | `"menu.file.jar.open.aliases"`                    |
| `File`>`Close Jar`                           | `"menu.file.jar.close.aliases"`                   |
| `File`>`Open Mappings...`                    | `"menu.file.mappings.open.aliases"`               |
| `File`>`Max Recent Projects`                 | `"menu.file.max_recent_projects.aliases"`         |
| `File`>`Save Mappings`                       | `"menu.file.mappings.save.aliases"`               |
| `File`>`Auto Save Mappings`                  | `"menu.file.mappings.auto_save.aliases"`          |
| `File`>`Close Mappings`                      | `"menu.file.mappings.close.aliases"`              |
| `File`>`Drop Invalid Mappings`               | `"menu.file.mappings.drop.aliases"`               |
| `File`>`Reload Mappings`                     | `"menu.file.reload_mappings.aliases"`             |
| `File`>`Reload Jar/Mappings`                 | `"menu.file.reload_all.aliases"`                  |
| `File`>`Export Source...`                    | `"menu.file.export.source.aliases"`               |
| `File`>`Export Jar...`                       | `"menu.file.export.jar.aliases"`                  |
| `File`>`Mapping Stats...`                    | `"menu.file.stats.aliases"`                       |
| `File`>`Configure Keybinds...`               | `"menu.file.configure_keybinds.aliases"`          |
| `File`>`Exit`                                | `"menu.file.exit.aliases"`                        |
| `Open Recent Project` menu                   | `"menu.file.open_recent_project.aliases"`         |
| `Save Mappings As...` menu                   | `"menu.file.mappings.save_as.aliases"`            |
| `Save Mappings As...`>`Enigma File`          | `"enigma:enigma_file.aliases"`                    |
| `Save Mappings As...`>`Enigma Directory`     | `"enigma:enigma_directory.aliases"`               |
| `Save Mappings As...`>`Enigma ZIP`           | `"enigma:enigma_zip.aliases"`                     |
| `Save Mappings As...`>`Tiny v2`              | `"enigma:tiny_v2.aliases"`                        |
| `Save Mappings As...`>`SRG File`             | `"enigma:srg_file.aliases"`                       |
| `View` menu                                  | `"menu.view.aliases"`                             |
| `Entry Tooltips` menu                        | `"menu.view.entry_tooltips.aliases"`              |
| `Entry Tooltips`>`Enable tooltips`           | `"menu.view.entry_tooltips.enable.aliases"`       |
| `Entry Tooltips`>`Allow tooltip interaction` | `"menu.view.entry_tooltips.interactable.aliases"` |
| `Languages` menu                             | `"menu.view.languages.aliases"`                   |
| `Server Notifications` menu                  | `"menu.view.notifications.aliases"`               |
| `Scale` menu                                 | `"menu.view.scale.aliases"`                       |
| `Stat Icons` menu                            | `"menu.view.stat_icons.aliases"`                  |
| `Themes` menu                                | `"menu.view.themes.aliases"`                      |
