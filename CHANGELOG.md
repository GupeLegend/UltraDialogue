# Changelog — UltraDialogue

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
