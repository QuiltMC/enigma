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

Since elements can have multiple search aliases, their translations can be lists. Aliases are separated by `;`.
For example, the `Dev` menu's aliases look like this in the translation file: `"Development;Debugging"`
This means that aliases may not contain the `;` character.

Some things to keep in mind when adding search aliases:
- elements' names are always searchable; there's no need to add their names to their aliases
- searching is case-insensitive, so there's no need to add variations that only differ in capitalization
- searching matches substrings, so there's no need to add a variation that's a substring of another variation,
just add the longer variation (note that the element name may be a substring of an alias, as is the case with `Dev`'s
`"Development"` alias)

If you'd like to add search aliases to an element that doesn't already have aliases, add its alias translation key to
the translation file.

#### Complete list of search alias translation keys
| Element                                                  | Translation Key                                       |
|----------------------------------------------------------|-------------------------------------------------------|
| `Dev` menu                                               | `"dev.menu.aliases"`                                  |
| `Dev`>`Show mapping source plugin`                       | `"dev.menu.show_mapping_source_plugin.aliases"`       |
| `Dev`>`Debug token highlights`                           | `"dev.menu.debug_token_highlights.aliases"`           |
| `Dev`>`Log client packets`                               | `"dev.menu.log_client_packets.aliases"`               |
| `Dev`>`Print mapping tree`                               | `"dev.menu.print_mapping_tree.aliases"`               |
| `Collab` menu                                            | `"menu.collab.aliases"`                               |
| `Collab`>`Connect to Server`                             | `"menu.collab.connect.aliases"`                       |
| `Collab`>`Disconnect`                                    | `"menu.collab.disconnect.aliases"`                    |
| `Collab`>`Start Server`                                  | `"menu.collab.server.start.aliases"`                  |
| `Collab`>`Stop Server`                                   | `"menu.collab.server.stop.aliases"`                   |
| `Decompiler` menu                                        | `"menu.decompiler.aliases"`                           |
| `Decompiler`>`Decompiler Settings`                       | `"menu.decompiler.settings.aliases"`                  |
| `Search` menu                                            | `"menu.search.aliases"`                               |
| `Search`>`Search All`                                    | `"menu.search.all.aliases"`                           |
| `Search`>`Search Classes`                                | `"menu.search.class.aliases"`                         |
| `Search`>`Search Methods`                                | `"menu.search.method.aliases"`                        |
| `Search`>`Search Fields`                                 | `"menu.search.field.aliases"`                         |
| `Crash History` menu                                     | `"menu.file.crash_history.aliases"`                   |
| `File` menu                                              | `"menu.file.aliases"`                                 |
| `File`>`Open Jar...`                                     | `"menu.file.jar.open.aliases"`                        |
| `File`>`Close Jar`                                       | `"menu.file.jar.close.aliases"`                       |
| `File`>`Open Mappings...`                                | `"menu.file.mappings.open.aliases"`                   |
| `File`>`Max Recent Projects`                             | `"menu.file.max_recent_projects.aliases"`             |
| `File`>`Save Mappings`                                   | `"menu.file.mappings.save.aliases"`                   |
| `File`>`Auto Save Mappings`                              | `"menu.file.mappings.auto_save.aliases"`              |
| `File`>`Close Mappings`                                  | `"menu.file.mappings.close.aliases"`                  |
| `File`>`Drop Invalid Mappings`                           | `"menu.file.mappings.drop.aliases"`                   |
| `File`>`Reload Mappings`                                 | `"menu.file.reload_mappings.aliases"`                 |
| `File`>`Reload Jar/Mappings`                             | `"menu.file.reload_all.aliases"`                      |
| `File`>`Export Source...`                                | `"menu.file.export.source.aliases"`                   |
| `File`>`Export Jar...`                                   | `"menu.file.export.jar.aliases"`                      |
| `File`>`Mapping Stats...`                                | `"menu.file.stats.aliases"`                           |
| `File`>`Configure Keybinds...`                           | `"menu.file.configure_keybinds.aliases"`              |
| `File`>`Exit`                                            | `"menu.file.exit.aliases"`                            |
| `Open Recent Project` menu                               | `"menu.file.open_recent_project.aliases"`             |
| `Save Mappings As...` menu                               | `"menu.file.mappings.save_as.aliases"`                |
| `Save Mappings As...`>`Enigma File`                      | `"enigma:enigma_file.aliases"`                        |
| `Save Mappings As...`>`Enigma Directory`                 | `"enigma:enigma_directory.aliases"`                   |
| `Save Mappings As...`>`Enigma ZIP`                       | `"enigma:enigma_zip.aliases"`                         |
| `Save Mappings As...`>`Tiny v2`                          | `"enigma:tiny_v2.aliases"`                            |
| `Save Mappings As...`>`SRG File`                         | `"enigma:srg_file.aliases"`                           |
| `View` menu                                              | `"menu.view.aliases"`                                 |
| `View`>`Languages` menu                                  | `"menu.view.languages.aliases"`                       |
| `View`>`Languages`>`German`                              | `language.de_de.aliases`                              |
| `View`>`Languages`>`English`                             | `language.en_us.aliases`                              |
| `View`>`Languages`>`Français`                            | `language.fr_fr.aliases`                              |
| `View`>`Languages`>`日本語`                                 | `language.ja_jp.aliases`                              |
| `View`>`Languages`>`简体中文`                                | `language.zh_cn.aliases`                              |
| `View`>`Server Notifications` menu                       | `"menu.view.notifications.aliases"`                   |
| `View`>`Server Notifications`>`No server notifications`  | `"notification.level.none.aliases"`                   |
| `View`>`Server Notifications`>`No chat messages`         | `"notification.level.no_chat.aliases"`                |
| `View`>`Server Notifications`>`All server notifications` | `"notification.level.full.aliases"`                   |
| `View`>`Stat Icons` menu                                 | `"menu.view.stat_icons.aliases"`                      |
| `View`>`Stat Icons`>`Include synthetic parameters`       | `"menu.view.stat_icons.include_synthetic.aliases"`    |
| `View`>`Stat Icons`>`Count fallback-proposed names`      | `"menu.view.stat_icons.count_fallback.aliases"`       |
| `View`>`Stat Icons`>`Enable icons`                       | `"menu.view.stat_icons.enable_icons.aliases"`         |
| `View`>`Stat Icons`>`Included types` menu                | `"menu.view.stat_icons.included_types.aliases"`       |
| `View`>`Stat Icons`>`Included types`>`Methods`           | `"type.methods.aliases"`                              |
| `View`>`Stat Icons`>`Included types`>`Fields`            | `"type.fields.aliases"`                               |
| `View`>`Stat Icons`>`Included types`>`Parameters`        | `"type.parameters.aliases"`                           |
| `View`>`Stat Icons`>`Included types`>`Classes`           | `"type.classes.aliases"`                              |
| `View`>`Entry Tooltips` menu                             | `"menu.view.entry_tooltips.aliases"`                  |
| `View`>`Entry Tooltips`>`Enable tooltips`                | `"menu.view.entry_tooltips.enable.aliases"`           |
| `View`>`Entry Tooltips`>`Allow tooltip interaction`      | `"menu.view.entry_tooltips.interactable.aliases"`     |
| `View`>`Selection Highlight` menu                        | `"menu.view.selection_highlight.aliases"`             |
| `View`>`Selection Highlight`>`Blink count (#)` menu      | `"menu.view.selection_highlight.blinks.aliases"`      |
| `View`>`Selection Highlight`>`Blink delay (#ms)...`      | `"menu.view.selection_highlight.blink_delay.aliases"` |
| `View`>`Themes` menu                                     | `"menu.view.themes.aliases"`                          |
| `View`>`Themes`>`Default`                                | `"menu.view.themes.default.aliases"`                  |
| `View`>`Themes`>`Darcula`                                | `"menu.view.themes.darcula.aliases"`                  |
| `View`>`Themes`>`Darcerula`                              | `"menu.view.themes.darcerula.aliases"`                |
| `View`>`Themes`>`Metal`                                  | `"menu.view.themes.metal.aliases"`                    |
| `View`>`Themes`>`System`                                 | `"menu.view.themes.system.aliases"`                   |
| `View`>`Themes`>`None (JVM Default)`                     | `"menu.view.themes.none.aliases"`                     |
| `View`>`Scale` menu                                      | `"menu.view.scale.aliases"`                           |
| `View`>`Fonts...`                                        | `"menu.view.font.aliases"`                            |
