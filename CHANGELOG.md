# Changelog — UltraDialogue

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
