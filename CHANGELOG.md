# Changelog — UltraDialogue

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
