# Changelog — UltraDialogue

## 1.1.4 — 2026-10-24

**The button pack now travels inside your server's pack, one jar runs on 26.1 to 26.3, and Bedrock
players can use every dialogue.** Dialogue files don't change.

### The pack goes into your server's pack

- UltraDialogue **no longer sends a resource pack** or opens a port (8083 is free again). On startup,
  before the other plugins start, it puts the button pack inside the plugin that builds your server's
  pack, so players get a single pack: **ResourcePackManager, Nexo, Oraxen, ItemsAdder, CraftEngine or
  EcoItems** (the first one installed, in that order). The buttons are in that pack on the same boot.
  ItemsAdder and CraftEngine only build their pack on command, so once the server is up the plugin runs
  `/iazip` or `/ce reload pack` — only when the buttons changed.
- `resource-pack.enabled` is gone. Configs from 1.1.2 still have `enabled: false` (it meant "don't
  send the pack yourself") and it's ignored, so the pack still gets installed. To turn the install off,
  use `install: none`.
- **No pack plugin?** The pack is left in `plugins/UltraDialogue/resourcepack/` with a README explaining
  how to load it, and the console shows a WARNING. After adding it to your own pack, set
  `resource-pack.merged-by-hand: true`.
- `buttons: auto` now means: drawn for players whose last server pack loaded fine, `[ text ]` for
  anyone who declined it or failed to download it.
- `config.yml`: new `resource-pack.install` (`auto`, `none` or one plugin's name) and
  `merged-by-hand`. Gone: `mode`, `host`, `port`, `url`, `force` and `prompt`, and the `pack-prompt`
  language line. Old keys left in a server's config are simply ignored.
- Tested with EcoItems, ResourcePackManager, CraftEngine and with none. Nexo, Oraxen and ItemsAdder follow
  their official documentation but couldn't be tested (paid plugins).

### Fixes

- **Drawn buttons showed as boxes (□□□) on 26.3.** Two textures of the button font were named `L.png` and
  `R.png`; Minecraft 26.3 rejects capital letters in resource paths and dropped the whole font. They're
  now `l.png` and `r.png`. Found with a real 26.3 client.
- Pack plugins that send the pack while the player is still connecting (CraftEngine) report nothing to
  other plugins, so those players always got `[ text ]`. With CraftEngine, a player now counts as having
  the buttons unless they decline the pack later.
- The `/ud` editor showed colour codes (`&7`, `&8`) in the descriptions of nodes and buttons instead of
  painting them. The lines come from the language file, so they're now read as coloured text.

### Bedrock

- Geyser shows dialogues as Bedrock forms. Bedrock players (detected through **Floodgate**, optional)
  always get the text buttons: Bedrock can't draw the pack's font, even if Geyser reports the Java pack
  as loaded.
- On a node with `layout:`, Bedrock players get the rows as **normal buttons**: text in a form can't be
  clicked, so before they couldn't answer at all.

### Paper, Purpur and Folia

- Runs on **Paper, Purpur and Folia** (`folia-supported: true`). Clicks, editor steps and console
  commands that touch a player now run on that player's own thread (on Folia, their region), and
  console commands from dialogue actions run on the global thread.
- On Folia, `/ud flags` and `/ud affinity` answer a moment later, from the player's thread. From RCON
  that reply is lost because the connection is already closed; in game or in the console it shows.

### 26.1 to 26.3

- One jar for **Paper 26.1, 26.2 and 26.3**: built against 26.1.2 with `api-version: '26.1'`. Tested on
  all three with a bot that reads and clicks the dialogues, 21 of 21 on each: first-time greeting and
  flags, going back, locked answers and affinity, free rows, `nav:` actions, opening another dialogue,
  permissions, closing, `/ud reload` and `/ud list`. Also checked: drawn buttons for a player who loaded
  the server's pack, `[ text ]` for one who declined it, and the drawn buttons with a real 26.3 client
  through CraftEngine.
- `website` in `plugin.yml` now points to the UltraWiki.

## 1.1.3 — 2026-10-15

**A button editor in `/ud`, a much smaller resource pack, and a cleaner, reorganised codebase.**
No change to how dialogue files are written or how conversations play out.

### Button editor

- **`/ud` (or `/ud editor`) now opens an in-game menu**: pick a dialogue, pick one of its nodes,
  and see its buttons (the main answers and the `nav:` row).
- For each button, admins can **edit its text** (typed in chat, with a cancel step), **change its
  icon** (only the 10 approved presets, or none), **change its size** (auto/1/2/3/4), **move it up
  or down** or, on a node using `layout:`, **move it to another row**, and **delete it** with a
  confirmation step.
- **Add a button** by picking an existing one as a template — its `goto`, `actions` and conditions
  are copied, then its text and icon are edited like any other button.
- **Test** opens the dialogue as it is currently saved; **Save** writes the buttons back to that
  node's `.yml` file; **Cancel**, or just closing the menu, discards anything unsaved.
- The editor only ever touches buttons. The NPC's own lines, conditions, actions and affinity are
  still edited in the dialogue's file, same as before; the menu says so.
- Saving rewrites only the edited node's `answers:`, `nav:` and `layout:` lines — every other node,
  every other key, and the rest of the file's comments are left exactly as they were.

### A much smaller resource pack

- The bundled button pack now only ships what's actually used: the **`liso` background and the
  `doble` border**. The jar dropped from ~570 KB to ~200 KB.
- Any older style or border name in a dialogue file (or in `config.yml`) still loads without
  errors: it falls back to `liso`/`doble` with a single, once-per-name console notice instead of a
  blank or missing-texture button. `resource-pack.textures: vanilla` (block-texture buttons) and
  the biome-scene panels are gone along with the artwork backing them.

### Its own data folder

- UltraDialogue now keeps its files in the normal **`plugins/UltraDialogue/`** folder instead of
  the shared `plugins/UltraView/UltraDialogue/` one. Upgrading from a version that used the shared
  folder moves everything over automatically the first time it starts (logged once), and removes
  the old `UltraView` folder once it's empty. No server owner action needed.

### Source code reorganised

- The source moved from one flat package into `command`, `config`, `migration`, `model`,
  `registry`, `gui` (with `gui/editor` for the new menu), `listener`, `pack`, `hook` and `util`,
  grouped by what each class does.
- Every identifier, comment and log message is now in English (dialogue files still accept both
  English and Spanish keys, as always).
- Added a Gradle (Kotlin DSL) build alongside `compilar.sh`, which no longer depends on a `_libs`
  folder shared with other plugins — each now downloads into its own `libs/`.

## 1.1.2

**Free row layouts, a clean double-frame theme, quest symbols and better navigation.**

For **Paper 26.1.2** with **Java 25**. No client mod required. The RPG button pack is optional.

### Free row layouts

- Added **`layout:` / `filas:`** on a node, for arrangements such as `layout: [1, 2, 2, 1]` or `[3, 1]`.
- Each row accepts 1–6 options. Answers are placed in order; leftovers get individual rows.
- These options are **clickable components in the dialogue body**, not a replacement for Minecraft's native button-grid API.
- Without the button pack, they appear as `[ text ]`. Leave out `layout:` to retain the native grid.
- **`nav:` becomes one row of real Minecraft buttons** below free-row options: Back, Exit and custom navigation answers can sit side by side.
- `row-width: full` aligns rows; `fit` and a numeric minimum width are also available. Explicit answer widths can override the shared layout.

### Simple RPG panels

- The default pack-drawn appearance is now **a flat dark background with a double border**: `style: liso`, `border: doble`, `look: filled`.
- `force: true` applies that same background and border to every layout panel. Native grid/nav controls keep their Minecraft appearance.
- Frames follow the option's icon colour: main quest green, side quest blue, riddle purple, questions white, rewards yellow, and locked options grey.
- `border-colors` customises the palette without a new pack. `force-colors: true` uses semantic colours; set it to `false` to preserve explicit per-dialogue colour overrides.
- Pack panels are **24 pixels tall**. Legacy style IDs/assets remain available for compatibility, but are not the active default theme.
- `buttons: auto` draws panels only after Minecraft confirms the pack loaded; `text` always uses text. `pack` forces panels for externally delivered packs and assumes the required font is present.
- The pack is bundled in the JAR. Internal delivery offers it after join, using port **8083** by default; a reachable address/port and client acceptance are still required.
- The pack darkens Minecraft's in-world menu background. This affects other in-world menus too, not only dialogues; the player's blur setting is unchanged.

### Quest symbols

- Replaced the old 1.1.1 sprite set with **10 named text presets**:
  - `quest` / `mision`: `!`
  - `main` / `principal`, `side` / `secundaria`, `riddle` / `acertijo`: `•` in green, blue or purple
  - `accept` / `aceptar`: `✓`
  - `back` / `volver`: `«`
  - `exit` / `salir`: `×`
  - `reward` / `premio`: `±`
  - `story` / `cuento`: `♪`
  - `chance` / `probabilidad`: `%`
- Combine presets: `icon: quest side` shows a gold `!` and a blue `•`. The last coloured preset chooses the panel frame colour.
- Symbols need no resource pack. Removed names such as `heart`, `gem`, `gift` and `shop` must be migrated; unknown names are skipped with a warning. Update old icon-only answers so they do not become blank labels.

### Locked answers and navigation

- Added **`locked:`** to answers with `if:`: a failed condition keeps the answer visible in grey rather than hiding it. Clicking it shows a hint/sound, without running its actions.
- `locked: true` uses the default language text. Locked answers are not randomly rotated out by `show:`.
- **Back** follows conversation history, including switches between dialogues; on the first screen it closes the talk.
- **Exit** closes the conversation. Navigation width is configurable with `screen.nav-width`.
- Added per-node **`title:` / `titulo:` / `título:`** for screen headings.
- In the native grid, a full-width answer (`size: 4`) is moved to the end. Validation warns about layouts/full-width combinations that cannot form the intended rows.
- Fixed text-only screens with Exit navigation: they now use a notice dialog instead of an invalid empty action list.

### Presentation limits

Free-row panels have no native hover highlight, and their click area follows the text line rather than the full drawing. The navigation row is native and retains its hover highlight. Custom client fonts/resource packs may affect text alignment.

### Updating from 1.1.1

1. Stop the server, replace the old JAR with **`UltraDialogue-1.1.2.jar`**, and restart using Java 25. Do not replace the plugin's data folder.
2. Review old icon names. Existing configs/dialogues are preserved; explicit old layout settings can override the new defaults. Copy only the settings you want to change.
3. If another plugin delivers a merged pack, import the current UltraDialogue ZIP, regenerate that provider's pack, disable UltraDialogue's own delivery, and use `buttons: pack`.
4. Use `/ud reload` for edited dialogue/configuration files, then close and reopen active conversations. Reload does not cancel existing conversation sessions or restart the pack provider; restart the server after changing pack delivery/port settings.

This release's verified server target is **Paper 26.1.2**. The older-client chat fallback does not imply compatibility with older servers. **Folia is not supported; Bedrock/Geyser is not verified.**

## 1.1.1 — 2026-09-26

### Button sizes
- **`size: 1` to `4`** on any answer: 20, 98, 150 and 204 px, like the pause menu buttons (report,
  Options, a medium one, Disconnect). Or **`width: N`** for an exact width (1–1024).
- **`columns:` per node**, so two half-width buttons can sit side by side on one screen only.
- An answer can have **no text, only an icon** (for `size: 1`).

### Icons
- **20 built-in pixel-art icons** for answers (`icon: quest`): quest, accept, question, talk, shop,
  secret, choice, gift, back, exit, staff, gem, sword, heart, skull, chest, key, map, star, book.
  Nine of them are animated.
- Drawn inside the text with Minecraft's sprite objects. The plugin **serves the pack itself** on
  port **8083**; it stacks with other packs.
- **Text fallback**: players who didn't load the pack (or use an old client) see a coloured symbol
  (`!`, `✔`, `?`, `$`...) instead of a missing texture. `icons.mode: auto | sprite | text`.
- **`/ud icons`** lists every icon.

### NPC face
- Head portraits (`npc`, `player:`, `texture:`) now show as a **2D face** from the skin next to the
  NPC's name. The 3D head item used before rendered as a dark silhouette.

### Deliveries
- **`has:` / `hand:`** conditions and **`take:` / `give:`** actions. Items: vanilla material (plain
  items only), `ultraboss:<id>`, `ultrarevive:<id>`, `pdc:<key>=<value>`, `item_model:<id>`, `name:<text>`.
- `take:` is **all or nothing**, main hand first; if it fails, the rest of the answer's actions are cut.
- **Anti-dupe:** an answer's conditions are checked again when it's clicked.

### Quests and story locks
- Conditions **`quest: completed <id>`**, **`quest: started <id>`**, **`quest: active <key>`**.
- Actions **`quest: complete <key>`**, **`quest: start <id>`**, **`quest: force <id>`**.
- New BeautyQuests stage type **`ULTRADIALOGUE`** (also in its in-game editor).
- **`DialogueSignalEvent`** — a Bukkit event any plugin can listen to.
- BeautyQuests is optional (`softdepend`); its classes live in `hook/bq` and only load if it's there.

### Placeholders
- `%ultradialogue_affinity_<id>%` and `%ultradialogue_flag_<flag>%` (PlaceholderAPI, optional).

### Also
- Spanish keys: `icono`, `tamaño` / `tamano`, `ancho`, `columnas` (per node), `tiene`, `mano`,
  `quitar`, `dar`, `mision` (`completada`, `en-curso`, `activa`, `completar`, `empezar`, `forzar`).

## 1.1 — 2026-09-26

### Affinity
- A **hidden friendship level** per player and per character, starting at 0. Answers change it
  (`affinity: +2`, `affinity: -3`, `affinity: =10`, or `affinity: kadir +2` for another character)
  and conditions read it (`affinity >= 20`, `affinity:kadir >= 20`).
- **Players are never told** it changed. Staff can see and edit it with `/ud affinity`.
- **Anti-farming:** a character only gives affinity once every 30 minutes and at most 3 points per
  conversation (both configurable, globally or per dialogue). Losing affinity is never limited.
- Stored on the player (PersistentDataContainer), like flags.

### Conversations that change
- **`variants:`** — several versions of a line; one is picked at random each time, never the same
  twice in a row. Each variant can have its own `if:`.
- **`show: N`** — only N of a node's questions appear, picked at random. Answers with
  **`always: true`** are never rotated out.

### Also
- New example **`kadir.yml`**: four friendship levels, rotating questions and a one-time gift.
- All new keys also accept Spanish (`afinidad`, `variantes`, `mostrar`, `siempre`,
  `cooldown-minutos`, `max-por-charla`).

## 1.0 — 2026-09-25

First public release, for **Minecraft 26.1.2**. It started as a private plugin called "Dialogos",
built for one server.

### What it does
- **NPC dialogue screens** using Minecraft's native dialogs (1.21.6+): portrait, text, and answers
  as buttons. No resource pack, no client mod.
- **Branching conversations in YAML**: nodes, answers, `next`, `start` rules.
- **13 actions**: goto, dialogue, menu, command, console, message, broadcast, actionbar, title,
  sound, flag, unflag, close.
- **Conditions** on answers and on where a talk starts: permissions, player flags and
  PlaceholderAPI comparisons.
- **Player flags** stored on the player (PersistentDataContainer).
- **Portraits**: NPC skin, any player's skin, a texture, or an item — with fallbacks.
- **FancyNpcs**: clicking a linked NPC opens its dialogue instead of its actions.
- **Entity tags**: any entity can talk with `dialogue:<id>`.
- **Chat fallback** for clients older than 1.21.6 (ViaVersion), or for everyone with `mode: chat`.
- **English and Spanish.**
- Data in `plugins/UltraView/UltraDialogue/`.

### Changes from the private plugin
- **Renamed** from Dialogos to UltraDialogue: command `/ud` (aliases `/dialogue`, `/dialogo`),
  permission `ultradialogue.admin` (the old `dialogos.admin` still grants it), package
  `mc.gupe.ultradialogue`.
- **Config and dialogue keys in English.** The Spanish ones still work, so old files load untouched.
- **Migration:** on first start, copies `plugins/Dialogos/dialogos/` and converts the old
  `config.yml` (setting `language: es`). The old folder is not deleted.
- **Player flags survive the rename:** the old `dialogos:marcas` key is read and moved.
- **Fixed:** "Goodbye" and exit buttons needed two clicks to close the screen.
