<p align="center"><img src="docs/Logo.png" alt="UltraDialogue" width="280"></p>

# UltraDialogue

[![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-brightgreen)](https://papermc.io)
[![Paper](https://img.shields.io/badge/Paper-26.1.2-blue)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-25-orange)](https://adoptium.net)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

**Real dialogue screens for your NPCs** — a portrait, what the character says, and the player's
answers as buttons. Every answer can branch the conversation, open a menu or run commands.

A Paper plugin by **Social Studio**.

It uses the **native dialog screens** Minecraft introduced in client version 1.21.6. The
verified server target for this JAR is **Paper 26.1.2 / Java 25**. No client mod is required.
Native buttons and plain free-row text need no resource pack; the optional RPG panel appearance
uses the button pack bundled with the plugin.

## Screenshots from a configured server

<p align="center">
  <img src="docs/gallery/01-quest-board.png" alt="Quest board with coloured frames" title="Quest board with coloured frames" width="49%">
  <img src="docs/gallery/02-free-row-layouts.png" alt="Free row layouts" title="Free row layouts" width="49%">
</p>
<p align="center">
  <img src="docs/gallery/03-multi-page-menus.png" alt="Multi-page menus with Back" title="Multi-page menus with Back" width="49%">
  <img src="docs/gallery/04-coloured-menus.png" alt="Coloured category menus" title="Coloured category menus" width="49%">
</p>
<p align="center">
  <img src="docs/gallery/05-confirmations-requirements.png" alt="Confirmations and requirements" title="Confirmations and requirements" width="49%">
  <img src="docs/gallery/06-grid-locked-entries.png" alt="Grids with locked entries" title="Grids with locked entries" width="49%">
</p>

Each image groups real frames of the same kind of screen. The individual frames are in
[`docs/screenshots/`](docs/screenshots/). NPCs, the world, quests, Codex content, fonts and other
server resource-pack customisations shown here are not supplied with the plugin.

---

## Features

- **Branching conversations** written in YAML: nodes, answers, and where each answer leads.
- **Actions on any answer:** go to another node or dialogue, open a menu, run a player or console
  command, send a message / title / action bar, play a sound.
- **Button sizes and icons**: four button sizes like the pause menu (from the tiny square to full
  width) and a set of quest symbols *(1.1.2)* — quest, main / side quest / riddle markers, accept,
  back, exit, reward, story and chance. Plain text: no resource pack.
- **The NPC's face** *(1.1.1)*: a 2D face from the NPC's skin next to its name.
- **Free layouts** *(1.1.2)*: `layout: [1, 2, 2, 1]` puts any number of buttons on each row — the
  shape Minecraft's own dialog grid can't do.
- **RPG buttons** *(1.1.2)*: with the optional button pack (offered after join when delivery is enabled), those
  buttons use a flat dark background and a double border coloured by their function. The same
  simple design applies to every NPC and chapter. Without the pack: `[ text ]`.
- **Locked answers and navigation** *(1.1.2)*: answers you can't pick yet show greyed out with a
  hint, a **Back** button that remembers the way, an **Exit** pinned to the bottom, and per-node
  titles.
- **Deliveries** *(1.1.1)*: check, take and give items — vanilla, UltraBoss trophies, or any item by
  its hidden tag, model or name. Taking is all-or-nothing and is checked again on click (no dupes).
- **Story locks and quests** *(1.1.1)*: conditions on BeautyQuests quests (`quest: completed 12`),
  its own BeautyQuests stage type, and placeholders for affinity and flags.
- **Conditions** that hide answers or change where a talk starts — permissions, player flags, and any
  PlaceholderAPI placeholder (`%vault_eco_balance% >= 1000`).
- **Player flags**: remember that someone already met a character, accepted a job or finished a
  step, and greet them differently next time. Stored on the player, no database.
- **Affinity** *(1.1)*: a hidden friendship level per player and per character. Kind answers raise
  it, rude ones lower it, and new greetings, questions and secrets unlock as it grows. Players are
  never told the number changed.
- **Conversations that never repeat** *(1.1)*: several variants per line (a different one every
  time) and questions that rotate, so talking to the same NPC twice doesn't feel scripted.
- **Portraits**: the NPC's own skin, any player's skin, a fixed texture, or any item.
- **FancyNpcs support out of the box**: link a dialogue to an NPC by name and clicking it opens the
  talk *instead of* its FancyNpcs actions. Your `npcs.yml` is never touched.
- **Any entity** can talk too, with a tag: `/tag @e[type=villager,limit=1,sort=nearest] add dialogue:merchant`.
- **Old clients** joining through ViaVersion (< 1.21.6) get the same conversation in chat, with
  clickable answers.
- **English and Spanish** built in.

## A dialogue

One file per character in `dialogues/`. The file name is the id.

```yaml
name: '&2&lThe Wandering Merchant'
npcs: [merchant]                    # FancyNpcs NPC names
portrait: 'npc | item:EMERALD'      # the NPC skin, or an emerald if it has none
start:
  - if: '!flag:met_merchant'        # first time...
    node: intro
  - node: hello                     # ...and every time after
nodes:
  intro:
    on-show: ['flag: met_merchant']
    text:
      - 'Psst! You, the one with empty pockets. Come closer.'
    next: hello                     # no answers = a "Continue" button
  hello:
    text: 'My stock changes every &fMonday&f.'
    answers:
      - text: '&2Show me your goods'
        actions: ['menu: shop']     # opens a DeluxeMenus menu
      - text: '&7Where do you get all this?'
        tooltip: '&7Hover text'
        goto: secret
      - text: '&8Not interested'
        goto: end
  secret:
    text: 'A good merchant never reveals his suppliers.'
```

`dialogues/example.yml` documents every option with comments.

### Actions

| Action | What it does |
|---|---|
| `goto: node` | Go to another node (`end` finishes the talk) |
| `dialogue: id [node]` | Switch to another dialogue |
| `menu: name` | Open a menu (runs `menu-command` from config.yml, DeluxeMenus by default) |
| `command: spawn` | Run a command as the player |
| `console: give %player% diamond 1` | Run a command from the console |
| `message:` · `broadcast:` · `actionbar:` | Text to the player / everyone / action bar |
| `title: Title\|Subtitle` | On-screen title |
| `sound: entity.villager.yes 1 1` | Also accepts `ENTITY_VILLAGER_YES` |
| `flag: x` · `unflag: x` | Set or remove a player flag |
| `affinity: +2` · `affinity: -3` · `affinity: =10` | Change affinity with this character (`affinity: kadir +2` for another one) |
| `close` | Close the screen |

When an answer ends the talk (opens a menu, runs a command…) the screen closes **before** the
actions run, so the menu stays open.

### Conditions (`if:`)

Every line of the list must match; inside a line, `||` means "or".

```
permission:group.vip       !permission:group.vip
flag:met_merchant          !flag:met_merchant
%vault_eco_balance% >= 1000          ( ==  !=  >=  <=  >  <  contains )
affinity >= 20             affinity:kadir >= 20
```

## Affinity and conversations that change

Every player has a hidden **affinity** with every character, starting at 0. Answers change it, and
conditions read it — so the same NPC can be cold with strangers and open up to friends:

```yaml
start:
  - if: 'affinity >= 25'
    node: friend
  - node: stranger

nodes:
  stranger:
    variants:                        # a different one each time, never the same twice in a row
      - 'Yes? What do you want?'
      - "I don't usually talk to strangers."
    show: 2                          # only 2 of these questions appear, picked at random
    answers:
      - text: '&7Hello. Who are you?'
        actions: ['affinity: +2']
        goto: who
      - text: '&8Out of my way.'
        actions: ['affinity: -3']
        goto: end
      - text: '&8Goodbye.'
        always: true                 # never rotated out
        goto: end
```

- **The player never sees the number** and is never told it changed. Staff can check it with
  `/ud affinity <player> <id>`.
- **Anti-farming:** a character only *gives* affinity once every `cooldown-minutes` (30 by default),
  and at most `max-per-talk` points (3) per conversation. Losing affinity has no limit.
- Defaults live in `config.yml` under `affinity:`; each dialogue can override them with its own
  `affinity:` section.
- It only exists in dialogues that use it. Menu NPCs that don't mention affinity behave exactly as
  before.
- A variant can have its own `if:` (`{text: ..., if: 'affinity >= 10'}`).

`dialogues/kadir.yml` is a complete example with four friendship levels, rotating questions and a
one-time gift.

## Button sizes and icons

```yaml
nodes:
  confirm:
    text: 'Will you take the job?'
    columns: 2                 # this node only: two buttons per row
    answers:
      - text: '&aYes'
        icon: accept
        size: 2                # half width, like "Options..."
        goto: accepted
      - text: '&cNo'
        icon: exit
        size: 2
        goto: end
      - text: ''
        icon: story            # size 1 fits only the icon, like the "report" button
        size: 1
        tooltip: '&7What is this job?'
        goto: details
```

| Size | Width | Like in the pause menu |
|---|---|---|
| `size: 1` | 20 px | the small square "report" button |
| `size: 2` | 98 px | "Options...", half width |
| `size: 3` | 150 px | a medium button |
| `size: 4` | 204 px | "Disconnect", full width |
| `width: 130` | any, 1–1024 | exact width |

Native buttons are 20 px tall. Pack-drawn free-row panels are 24 px tall. `columns:` works on
the native grid for the whole dialogue or a single node; `layout:` controls free rows.

**Icons** (`/ud icons` lists them in game). Since 1.1.2 they're **text symbols**, so they look the
same with or without a resource pack:

| Name | English | Symbol | Meaning |
|---|---|---|---|
| `mision` | `quest` | `!` (orange) | there's a quest |
| `principal` | `main` | `•` (green) | marker: main quest |
| `secundaria` | `side` | `•` (blue) | marker: side quest |
| `acertijo` | `riddle` | `•` (purple) | marker: riddle |
| `aceptar` | `accept` | `✓` (green) | accept |
| `volver` | `back` | `«` | back |
| `salir` | `exit` | `×` (red) | exit |
| `premio` | `reward` | `±` (yellow) | quest rewards |
| `cuento` | `story` | `♪` (light blue) | a story the NPC tells for a quest |
| `probabilidad` | `chance` | `%` | something may happen (or drop) by chance |

**Several icons on one button**, separated by spaces: `icon: quest side` shows `! •` with the blue
marker. `[quest, side]` and `quest+side` work too.

> **1.1.2 replaced the 20 pixel-art icons of 1.1.1** (`heart`, `gem`, `gift`, `star`, `shop`…). An icon
> that no longer exists doesn't break anything: the button shows without it and the console warns once
> per dialogue. Text symbols need no pack. `resource-pack.enabled: false` disables the internal
> button-pack server/public-IP lookup; use it for text-only presentation or when another plugin
> delivers a merged pack. Update removed icon-only answers so their labels are not blank.

**The NPC's face:** portraits `npc`, `player:Name` and `texture:...` now show as a 2D face from the
skin, next to the NPC's name. The client renders the face; player-name portraits can also use
asynchronous profile resolution through the server API. `item:` portraits show the item on the left.

## Locked answers, Back and Exit

```yaml
nodes:
  quests:
    title: '&6Quests of Brann'        # screen heading for this node only
    text: 'Pick a job.'
    columns: 2
    answers:
      - text: 'Side quest A'
        icon: quest
        size: 2
        goto: job_a
      - text: 'Friends only'
        icon: quest
        size: 2
        if: 'affinity >= 20'
        locked: '&7Brann needs to trust you more.'   # greyed out instead of hidden
        goto: job_b
      - text: 'Main quest'
        icon: quest
        size: 4                         # full width: moved to the last row by itself
        goto: main
    nav:
      - back                            # previous screen of this conversation
      - text: '&eReward'                # any answer works as a nav button
        actions: ['console: give %player% emerald 1']
      - exit                            # pinned to the bottom of the screen
```

- **`locked:`** needs an `if:`. While the condition fails the answer shows in `screen.locked-color`
  and clicking it only shows the hint in the action bar. `locked: true` uses the language's text.
  It's re-checked on every click like any answer, so nothing runs while it's locked.
- **`back`** walks back through the screens of this conversation, even into another dialogue. On
  the first screen it closes the talk (it's always shown, so the grid keeps its shape).
- **`exit`** goes in the footer. Minecraft only has room for **one** fixed button there, so `back`
  and the rest of `nav:` go in the last row of the grid.

## Free layouts (`layout:`)

```yaml
nodes:
  menu:
    title: '&6Quests of Brann'
    text: 'What do you want to ask?'
    layout: [1, 2, 2, 1]          # buttons per row, top to bottom
    answers:
      - {text: '&fMain quest', icon: quest main, goto: main}
      - {text: '&fSide quest', icon: quest side, goto: q1}
      - {text: '&fSide quest', icon: quest side, goto: q2}
      - {text: '&fRiddle', icon: quest riddle, if: 'affinity >= 10', locked: '&7Not yet.', goto: q3}
      - {text: '&fA story', icon: story, goto: q4}
      - {text: '&7Just passing by', goto: end}
    nav:                           # real buttons, all on one row, in this order
      - back
      - exit
      - {text: '&eReward', icon: reward, actions: ['console: give %player% emerald 1'], goto: menu}
```

- Rows are filled with the answers in order. Answers left over go one per row.
- **`nav:` buttons are real Minecraft buttons, all on one row** at the bottom, in the order you write
  them — so `back`, `exit` and a "Reward" sit side by side (with columns = number of buttons, the
  native grid allows it). Without `nav:`, a "Goodbye" button goes at the bottom.
- **How it works:** Minecraft only lets servers lay out real buttons in a uniform grid, but the
  text in the dialog body is clickable and every line is free. In this mode each button is a piece
  of text with its own click, drawn as `[ text ]` and padded with spaces so all rows line up. The
  plugin measures the text with the real Minecraft font, so columns stay aligned within a few pixels.
- **Trade-off:** the text buttons don't light up on hover like real ones (the tooltip still shows);
  the `nav:` row does, since those are real. If you want every button real, leave `layout:` out.
- Use `locked:` instead of a bare `if:` when you can: a hidden answer shifts every button after it to
  another row. The console warns when the number of answers doesn't match the layout.
- Change the look in `config.yml` → `screen.layout`: `button-format` / `locked-format` (`{text}` is the
  answer) and `gap` (spaces between buttons).

### RPG buttons

Players who loaded the plugin's pack see every `layout:` button as a panel: a background, a border
tinted by the button's type, and the text centred on top. Everyone else sees `[ text ]`. Nothing
else changes — the pack only draws.

```yaml
# config.yml → screen.layout
buttons: auto           # auto = loaded pack only · pack = merged/external pack · text = plain text
row-width: full
look: filled
style: liso
border: doble
force: true             # same background and border on every dialogue
force-colors: true      # semantic colours, ignoring per-dialogue colour overrides
border-colors:
  default: '#FFFFFF'    # question / no icon
  mision: '#F2B21E'
  principal: '#4CD964'
  secundaria: '#4F8BFF'
  acertijo: '#B36BFF'
  aceptar: '#4CD964'
  volver: '#C6E86B'
  salir: '#E04B4B'
  premio: '#FFD23F'
  cuento: '#7FD6FF'
  probabilidad: '#FF9F43'
  locked: '#5A5A5A'
```

The active appearance is **simple for all panels**: no biome scenes, no themed textures, just a
flat dark interior and a double frame. The last coloured icon sets the frame colour:
`icono: mision secundaria` draws a gold `!`, a blue `•`, and a blue frame. Locked panels are grey.

Legacy style IDs and assets remain accepted for compatibility. To restore per-dialogue `style` /
`border`, set `force: false`; to enable `border-color` / `fill-color` overrides, set
`force-colors: false`. The default uniform theme needs neither. Native `nav:` buttons are unchanged.

**How:** the pack adds a font, `ultradialogue:boton`, whose "letters" are button pieces — caps,
segments of 1 to 128 px and negative spaces. The plugin lays them out to the exact width, draws the
border on top (tinted with the text colour, so any colour works without touching the pack) and
centres the label. The pack is served by the plugin on port 8083 and stacks with other packs.

Buttons are 24 px tall (the background inside a 4 px frame). `row-width: full | fit | <px>` chooses
between rows that line up and rows as wide as their text. The pack also darkens Minecraft's in-world
menu background so the world behind the dialogue doesn't distract (every in-world menu is affected).

**Limits (Minecraft's, not the pack's):** panels don't light up on hover, and only the text line in
the middle of the panel is clickable. The `nav:` row is made of real buttons and does light up.

### What the screen can and can't do

Minecraft splits a dialog's buttons into rows of `columns` buttons each. **Only one leftover button
at the very end gets a row of its own.** So:

| Layout | Possible? |
|---|---|
| 2 + 2 + 1 (full-width last) | ✅ — UltraDialogue moves the `size: 4` answer there for you |
| 1 + 2 + 2 (full-width first) | ❌ with real buttons · ✅ with `layout:` |
| 2 + 2 + 2 + Exit fixed at the bottom | ✅ |
| Two fixed buttons at the bottom | ❌ — one fits; put the rest in `nav:` |
| Two-line buttons | ❌ — buttons are one line, 20 px tall |
| Three small buttons on one row (Back · Exit · Reward) | ✅ with `layout:` + `nav:` |
| Any row shape (1-2-2-1, 3-1, ...) | ✅ with `layout:` (text buttons) |

The pause menu does 1-2-2-1 with real buttons because it's built into the client; server dialogs
can't ask for that, which is why `layout:` uses text buttons instead.

## Deliveries

| Condition | Action |
|---|---|
| `has: IRON_INGOT 16` (`tiene:`) — anywhere in the inventory | `take: IRON_INGOT 16` (`quitar:`) — all or nothing |
| `hand: IRON_INGOT 16` (`mano:`) — main hand only | `give: IRON_INGOT 16` (`dar:`) |

Items can be written as:

| Format | Matches |
|---|---|
| `IRON_INGOT` / `minecraft:iron_ingot` | that vanilla item — **only plain ones**: items with a plugin's hidden tag or their own model don't count, so a plain item never takes an UltraBoss trophy |
| `ultraboss:<id>` | an UltraBoss item (also the ones from its older names) |
| `ultrarevive:self` / `boost` / `end` | an UltraRevive totem |
| `pdc:<namespace:key>=<value>` | any plugin's hidden tag |
| `item_model:<namespace:model>` | by item model |
| `name:&6Cellar Key` (`nombre:`) | by display name (last resort) |

```yaml
deliver:
  text: 'Did you bring the iron for the forge?'
  answers:
    - text: '&aHand over 16 iron ingots'
      icon: accept
      if: 'has: IRON_INGOT 16'
      actions:
        - 'take: IRON_INGOT 16'
        - 'give: EMERALD 4'
        - 'affinity: +3'
      goto: thanks
    - text: "&8I don't have them yet"
      if: '!has: IRON_INGOT 16'
      goto: end
```

- **`take:` is atomic**: it counts first and takes nothing if there isn't enough. Main hand first,
  then the rest of the inventory.
- **If `take:` fails, the rest of that answer's actions are cut** — a reward is never given without
  collecting first. The player gets the `item-missing` message.
- **Conditions are checked again when the button is clicked.** Open the dialogue with 16 ingots,
  drop them, press "Hand over": nothing is taken, nothing is given, the screen redraws.
- `give:` builds vanilla items itself; UltraBoss and UltraRevive items are given through their own
  command, so they come out with every tag, name and texture.
- A start rule can react to what the player is holding: `if: 'hand: IRON_INGOT 16'`.

## Story locks and BeautyQuests

Use quests (or flags, or affinity with another character) to decide what an NPC says — so players
can't skip to the end of the story:

```yaml
start:
  - if: '!quest: completed 12'
    node: too_early              # "What are you doing here? You haven't even found the key."
  - node: final_scene
```

| Condition | True when |
|---|---|
| `quest: completed <id>` (`mision: completada`) | the player finished that BeautyQuests quest |
| `quest: started <id>` (`mision: en-curso`) | it's started and not finished |
| `quest: active <key>` (`mision: activa`) | the player is **right now** at an `ULTRADIALOGUE` stage with that key |

| Action | What it does |
|---|---|
| `quest: complete <key>` (`mision: completar`) | completes every `ULTRADIALOGUE` stage with that key the player is on |
| `quest: start <id>` (`mision: empezar`) | starts the quest, respecting its requirements |
| `quest: force <id>` (`mision: forzar`) | starts it without requirements |

**The `ULTRADIALOGUE` stage type.** UltraDialogue registers its own stage in BeautyQuests, so an NPC
with a dialogue can take part in quests natively:

```yaml
# in the BeautyQuests quest file
'3':
  stageType: ULTRADIALOGUE
  key: brann_iron
  npc: brann                  # optional, only for the description
  customText: '§7Bring §f16 iron ingots §7to §fBrann'
```

```yaml
# in the dialogue
- text: '&aHand over the iron'
  icon: accept
  if: ['quest: active brann_iron', 'has: IRON_INGOT 16']
  actions: ['take: IRON_INGOT 16', 'quest: complete brann_iron']
```

It can also be created from BeautyQuests' in-game editor (it asks for the key). Two quests on the
same key both advance.

`quest: complete` fires a plain Bukkit event, **`DialogueSignalEvent(player, key)`** — any plugin can
listen to it, with or without BeautyQuests.

**Without BeautyQuests** everything still loads: quest conditions are false, `start`/`force` do
nothing, and the console says so once per dialogue.

## Placeholders

With PlaceholderAPI:

| Placeholder | Value |
|---|---|
| `%ultradialogue_affinity_<id>%` | the player's affinity with that character |
| `%ultradialogue_flag_<flag>%` | `true` / `false` |

Use them as real quest requirements in BeautyQuests (`placeholderRequired`), to lock menu items in
DeluxeMenus, or anywhere else.

## Commands and permissions

All administrative commands require `ultradialogue.admin` (op by default). Aliases: `/ud`,
`/dialogue`, `/dialogo`. The legacy `dialogos.admin` permission grants it too.

| Command | |
|---|---|
| `/ud open <id> [player] [node]` | Open a dialogue by hand — works from console and from other plugins |
| `/ud reload` | Reload config, language and every dialogue |
| `/ud list` | Loaded dialogues and their NPCs |
| `/ud flags <player> [clear [flag]]` | See or clear a player's flags |
| `/ud affinity <player> [id] [set\|add\|reset] [n]` | See or change affinity (staff only; `add` ignores the anti-farming limits) |
| `/ud icons` | Every icon with its name, drawn the way that player sees them |

Players need no permission to talk to NPCs.

## Requirements

| | |
|---|---|
| **Paper** | 26.1.2 (verified server target) |
| **Java** | 25 |

The older-client ViaVersion fallback does not imply support for older servers.
**Folia is not supported. Bedrock/Geyser behaviour is not verified.**

Everything else is optional and detected at runtime:

| Plugin | Used for |
|---|---|
| FancyNpcs | Clicking an NPC opens its dialogue |
| PlaceholderAPI | Placeholders in text, actions and conditions |
| DeluxeMenus | The `menu:` action (any menu plugin works — change `menu-command`) |
| ViaVersion | Sends old clients the chat version |
| BeautyQuests 2.1 | Quest conditions and actions, the `ULTRADIALOGUE` stage type |

## Install

Drop the jar in `plugins/` and start the server. Three example dialogues are created.

Symbols do not need a pack. For internally served RPG panels, configure a reachable public
address and port (**8083** by default). A bind failure is logged, but a firewall/NAT block may only
be visible as a client download failure. In `buttons: auto`, players get panels only after a
successful load; refusal/download failure leaves the text presentation. `buttons: pack` instead
assumes an external pack provider has delivered the font.

Data lives in **`plugins/UltraView/UltraDialogue/`**, the shared folder of the Ultra plugin family.
Your config and dialogues are never overwritten on update.

When you edit a dialogue, run `/ud reload`. If something is wrong the console tells you **which file,
which node and which reference** is broken. Close and reopen active conversations after reloading
their definitions: `/ud reload` does not cancel existing sessions. Pack delivery/port changes
require a server restart; reload does not restart the pack provider.

---

## Pack delivery and updates

For an external provider such as EcoItems, merge the current UltraDialogue ZIP, regenerate that
provider's pack and configure:

```yaml
resource-pack:
  enabled: false
screen:
  layout:
    buttons: pack
```

Merge these settings into the existing config; do not replace the whole file or duplicate its
`screen:` section. `buttons: pack` assumes the font is present and cannot confirm an external
provider's status. For the plugin's `mode: url`, serve the exact bundled pack bytes: its hash is
computed from the embedded ZIP, not from an arbitrary externally merged pack.

On upgrade, stop the server, replace only the JAR, and restart. Preserve
`plugins/UltraView/UltraDialogue/` and player data. Existing explicit layout settings can override
the new defaults. Add/update only the chosen configuration keys; migrate removed icon names.

The bundled pack darkens the background of other in-world menus too; this is not dialogue-only.
It does not change the player's blur setting. Custom fonts may alter label widths/alignment.

## Network use

Internal delivery serves the ZIP over HTTP on the configured port. `host: AUTO` queries
`api.ipify.org` for the public IP; `resource-pack.enabled: false` disables the provider and that
lookup. Player-name portraits can use asynchronous profile resolution via the server API.

## Building

No Gradle, no Maven — just `javac`.

```bash
bash compilar.sh          # -> a nearby Jars/ folder, or build/ when cloned standalone
```

Dependencies download to `../_libs/` on first run. You need **JDK 25**: Paper 26 is compiled for
Java 25. Build this release with JDK 25: the script does not set --release, so a newer JDK
can increase the bytecode requirement. With an older JDK `javac` fails with a `cannot access Player` error that looks like an
API problem but isn't.

## Project layout

```
src/main/java/mc/gupe/ultradialogue/
  UltraDialogue.java    startup and reload
  UltraView.java        the shared plugins/UltraView/<Plugin>/ folder
  Migration.java        imports data from the private plugin this one came from
  Registry.java         reads dialogues/ and validates every reference
  Dialogue.java         the model: dialogue -> nodes -> answers
  Screen.java           draws each turn (native dialog or chat) and receives the answer
  Actions.java          runs actions
  Conditions.java       evaluates if:
  Flags.java            per-player flags (PersistentDataContainer)
  Affinity.java         per-player, per-character affinity and its anti-farming limits
  Portraits.java        the item next to the text (item: portraits)
  Icons.java            the 10 named text-symbol presets and the NPC's 2D face
  PackManager.java      serves the button pack and tracks confirmed loads
  PackServer.java       the small HTTP server behind it
  NpcHook.java          the FancyNpcs click, by reflection
  Listeners.java        entity tags, click cooldown, quit
  Command.java          /ud
  Items.java            has / hand / take / give and the item formats
  QuestBridge.java      what UltraDialogue needs from a quest plugin
  DialogueSignalEvent   the event behind "quest: complete"
  hook/bq/              BeautyQuests: the bridge and the ULTRADIALOGUE stage (loaded only if present)
  hook/papi/            the PlaceholderAPI expansion (loaded only if present)
  Glyphs.java           width of every character of the Minecraft font (for layout:)
  Buttons.java          builds the RPG panels from the pack's font
  Lang.java / Text.java messages, colours, placeholders

src/main/resources/
  config.yml
  lang/{en,es}.yml
  dialogues/{example,merchant,kadir}.yml
  resourcepack.zip           the RPG button pack (built by TexturePacks/UltraDialogue/generar_pack.py)
  glyph-widths.bin           width of every character of the Minecraft font, in half pixels
```

## Notes for contributors

**Every screen carries a token.** A stale button — from a previous screen, an old chat line, or
from a different conversation session — does nothing. If you touch `Screen`, every `show()` must mint a new token.

**The native screen uses `afterAction(NONE)`, not `WAIT_FOR_RESPONSE`.** With `WAIT_FOR_RESPONSE`,
closing the dialog sends the client back to the screen it had *before* the wait — the same dialog —
so "Goodbye" looked like it didn't work the first time. Every path in `answer()` must end in `show()`
or `close()`.

**Classes that import another plugin live in `hook/` and are loaded by name** only when that plugin
is enabled. Never import BeautyQuests or PlaceholderAPI classes from the core: without them the JVM
would throw `NoClassDefFoundError` and the whole plugin would fail to start.

**`layout:` buttons are body text, not buttons.** The client hands clicks on dialog body text to
`DialogScreen.runAction`, the same path as a real button, so they arrive as `custom_click_action`.
Each button is one component carrying the click; its children (padding included) inherit it, so the
text-line padding is clickable. The drawn panel is taller than that line, so do not assume a full rectangular GUI hitbox. `Glyphs` is generated from the 26.1.2 client font; if Mojang changes the
font, regenerate it or rows drift by a few pixels.

**Conditions are checked twice**: when the screen is drawn and again on click (`Screen.answer`).
Don't remove the second check — it's what stops item dupes.

**Panel delivery needs confirmation.** In `buttons: auto`, the plugin draws panel glyphs only
after `SUCCESSFULLY_LOADED` for its own pack UUID. The ten icon presets are plain text and do not
depend on that status. `buttons: pack` deliberately bypasses confirmation for merged/external
packs; using it without the font can produce missing glyphs.

**Never ask Mojang for a skin on the main thread.** `player:` portraits are resolved in the
background when dialogues load and cached.

**Optional integrations go through reflection.** FancyNpcs, PlaceholderAPI and ViaVersion are looked
up by name, so the plugin compiles and starts without them. `NpcHook` uses the FancyNpcs 2.12.1
signatures; if a future version changes them, the console says so and NPCs fall back to their normal
actions.

**The flag key carries the plugin name.** `new NamespacedKey(plugin, "flags")` is stored as
`ultradialogue:flags`. This plugin started as a private one called "Dialogos", so `Flags` still reads
`dialogos:marcas` and moves it over. Don't remove that bridge.

**Both English and Spanish keys are accepted** in dialogue files (`text`/`texto`, `goto`/`ir`,
`answers`/`respuestas`…), so the private plugin's files load untouched.

Code comments are written in Spanish.

## License

[MIT](LICENSE) © 2026 Social Studio
